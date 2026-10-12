package com.slm.multilevelcache.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.slm.multilevelcache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 多级缓存管理器
 *
 * <p>除了 Spring {@link CacheManager} 的职责外，还额外承担一个关键角色：
 * <b>L1 失效广播的接收端</b>。收到其他节点发布的失效消息时，通过 {@link #evictLocal}
 * 清掉本节点的本地缓存。</p>
 *
 * <p>每个 cache name 对应一个独立的 L1(Caffeine) 与 L2 key 前缀。</p>
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    private final Map<String, MultiLevelCache> cacheMap = new ConcurrentHashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;

    private final CacheProperties properties;

    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate, CacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Cache getCache(String name) {
        // computeIfAbsent 保证同一 cache name 在本节点只有一个 L1 实例，
        // 否则广播来的失效消息会找不到真正持有数据的那个实例
        return this.cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }

    private MultiLevelCache createCache(String name) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> local = Caffeine.newBuilder()
                .maximumSize(properties.getLocalMaximumSize())
                // L1 刻意短于 L2：广播丢失时，本地缓存能靠 TTL 自行恢复一致
                .expireAfterWrite(properties.getLocalTtl())
                .recordStats()
                .build();
        log.info("创建多级缓存：name={}, L1 ttl={}, L2 ttl={}",
                name, properties.getLocalTtl(), properties.getRemoteTtl());
        return new MultiLevelCache(name, local, this.redisTemplate, this.properties);
    }

    /**
     * 清理本节点的 L1，不触碰 L2。
     *
     * <p>收到广播失效消息时调用：发起方节点已经删过 L2 了，各节点只需各自清理 L1。</p>
     *
     * @param cacheName 缓存名
     * @param key       缓存 key
     */
    public void evictLocal(String cacheName, Object key) {
        MultiLevelCache cache = this.cacheMap.get(cacheName);
        if (cache == null) {
            // 该节点从未使用过这个缓存，不可能有陈旧数据
            return;
        }
        cache.evictLocal(key);
        log.debug("收到失效广播，已清理本地缓存：cache={}, key={}", cacheName, key);
    }

    /**
     * 清空本节点某个缓存的 L1
     */
    public void clearLocal(String cacheName) {
        MultiLevelCache cache = this.cacheMap.get(cacheName);
        if (cache != null) {
            cache.evictAllLocal();
        }
    }

    /**
     * 清空本节点所有 L1，用于本地缓存全量预热前的兜底
     */
    public void clearAllLocal() {
        this.cacheMap.values().forEach(MultiLevelCache::evictAllLocal);
    }

    /**
     * 本节点已创建的全部多级缓存实例，供一致性对账任务遍历
     */
    public Collection<MultiLevelCache> getCaches() {
        return Collections.unmodifiableCollection(this.cacheMap.values());
    }

    /**
     * 对全部缓存执行 L1/L2 一致性对账
     *
     * @return 清理掉的陈旧条目数
     */
    public int reconcile() {
        int total = 0;
        for (MultiLevelCache cache : this.cacheMap.values()) {
            int before = (int) cache.localSize();
            cache.reconcileLocalAgainstRemote();
            total += Math.max(0, before - (int) cache.localSize());
        }
        return total;
    }

}