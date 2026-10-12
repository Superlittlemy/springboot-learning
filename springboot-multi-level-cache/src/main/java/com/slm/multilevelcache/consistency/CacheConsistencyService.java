package com.slm.multilevelcache.consistency;

import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;

/**
 * 缓存一致性服务
 *
 * <p>本模块的核心约定：<b>数据库是唯一真相源，缓存是派生数据</b>。
 * 采用 Cache-Aside 模式，写操作统一走「先更新数据库，再删除缓存」，
 * 绝不允许出现「先删缓存再写库」的顺序（那会放大并发读回填旧值的窗口）。</p>
 *
 * <p>一致性由四层机制共同保证，分别覆盖不同故障场景：</p>
 *
 * <ol>
 *   <li><b>删除缓存而非更新缓存</b>：删除是幂等的，不会出现「写了一半的新值」；
 *       读线程回填时必然从数据库取到已提交的新值。</li>
 *   <li><b>广播失效</b>：L1 私有，Redis 删不到，靠 Pub/Sub 通知所有节点清理 L1。</li>
 *   <li><b>延迟双删</b>：兜住「读线程在删缓存之后、写缓存之前把旧值回填」的并发竞态。</li>
 *   <li><b>TTL 自愈</b>：任何一次失效广播丢失，都会由 TTL 过期自动恢复一致，
 *       所以 L1 的 TTL 必须短于 L2。</li>
 * </ol>
 *
 * <p>关于最终一致性的边界：这套方案保证的是<b>最终一致</b>，且只在单 Redis 主库、
 * 不发生网络分区的前提下成立。Pub/Sub 本身不持久化，节点重连期间的消息会丢，
 * 所以 TTL 自愈不是可选项而是必需项。涉及资金/库存扣减的场景不应只依赖缓存，
 * 需要在数据库层加唯一约束或乐观锁兜底。</p>
 */
@Slf4j
@Service
public class CacheConsistencyService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MultiLevelCacheManager cacheManager;

    @Resource
    private CacheProperties properties;

    @Resource
    private ThreadPoolTaskScheduler taskScheduler;

    /**
     * 节点标识，仅用于排查
     */
    @Value("${spring.application.name:multi-level-cache}@${server.port:0}")
    private String nodeId;

    /**
     * 写操作后的标准失效流程（先更新数据库，再调用本方法）。
     *
     * <p>若当前处于事务中，失效动作会被推迟到<b>事务提交之后</b>执行。
     * 因为在事务提交前删缓存，并发读线程会查到尚未提交的新数据并回填，
     * 提交后缓存里就成了「读到了未提交数据」的脏值，推迟到 afterCommit 可以彻底避免这一点。</p>
     */
    public void evict(String cacheName, Object key) {
        runAfterCommit(() -> doEvict(cacheName, key));
    }

    /**
     * 清空某个缓存名的全部内容
     */
    public void evictAll(String cacheName) {
        runAfterCommit(() -> doEvictAll(cacheName));
    }

    /**
     * 事务提交后再执行；无事务时立即执行
     */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    /**
     * 实际的失效动作：延迟双删 + 广播
     */
    private void doEvict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }

        if (properties.isDelayedDoubleDeleteEnabled()) {
            // 第一次删除：挡住绝大多数读线程
            cache.evict(key);
            // 第二次删除：等读写竞态窗口过去后，再兜一次底
            scheduleDelayedEvict(cacheName, key);
        } else {
            cache.evict(key);
        }

        // 广播：让所有节点（含自己）确认 L1 已清
        publish(CacheInvalidateMessage.of(cacheName, key, nodeId));
    }

    private void doEvictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
        publish(CacheInvalidateMessage.ofAll(cacheName, nodeId));
    }

    /**
     * 发布失效广播。发布失败不能影响写操作，缓存最终一致由 TTL 兜底。
     */
    private void publish(CacheInvalidateMessage message) {
        try {
            redisTemplate.convertAndSend(properties.getInvalidateChannel(), message);
        } catch (Exception e) {
            log.warn("发布缓存失效广播失败，将依赖 TTL 过期恢复一致：{}", e.getMessage());
        }
    }

    /**
     * 延迟二次删除
     *
     * <p>需要延迟的原因：写线程删完缓存后，并发读线程可能正好在事务提交前
     * 已经查到了旧数据，此时它正在把旧值写回缓存。这个窗口通常只有几毫秒，
     * 延迟几百毫秒后再删一次即可覆盖。</p>
     */
    private void scheduleDelayedEvict(String cacheName, Object key) {
        Duration delay = properties.getDoubleDeleteDelay();
        long delayMillis = delay.toMillis();
        if (delayMillis <= 0) {
            return;
        }
        taskScheduler.schedule(() -> {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.evict(key);
                }
                // 二次删除同样广播，否则其它节点的 L1 仍可能残留被回填的旧值
                publish(CacheInvalidateMessage.of(cacheName, key, nodeId));
                log.debug("延迟双删完成：cache={}, key={}", cacheName, key);
            } catch (Exception e) {
                log.warn("延迟双删执行失败：{}", e.getMessage());
            }
        }, new Date(System.currentTimeMillis() + delayMillis));
    }

    /**
     * 处理来自其它节点的失效广播。
     *
     * <p>Redis Pub/Sub 会把发布者的消息回环给自己，因此这里必须再清一次本节点 L1：
     * 本节点可能正好在消息到达前把旧值回填进了 L1。</p>
     */
    public void onInvalidateMessage(CacheInvalidateMessage message) {
        if (message == null || message.getCacheName() == null) {
            return;
        }
        try {
            if (message.getType() == CacheInvalidateType.PATTERN) {
                cacheManager.clearLocal(message.getCacheName());
            } else {
                cacheManager.evictLocal(message.getCacheName(), message.getKey());
            }
            log.debug("已处理失效广播：cache={}, key={}, 来自={}",
                    message.getCacheName(), message.getKey(), message.getOriginNode());
        } catch (Exception e) {
            log.warn("处理失效广播失败：{}", e.getMessage());
        }
    }

    /**
     * 兜底对账：定期清理 L1 中在 L2 已经不存在的条目。
     *
     * <p>Pub/Sub 丢消息、或应用在广播到达前重启，都可能留下 L1/L2 不一致。
     * 这个低频任务保证系统最终收敛，是整个一致性方案的最后一道防线。</p>
     */
    @Scheduled(fixedDelayString = "${multi-level-cache.reconcile-interval-millis:60000}")
    public void reconcile() {
        int cleaned = cacheManager.reconcile();
        if (cleaned > 0) {
            log.info("缓存一致性对账完成，清理 {} 条 L1 陈旧数据", cleaned);
        }
    }

}