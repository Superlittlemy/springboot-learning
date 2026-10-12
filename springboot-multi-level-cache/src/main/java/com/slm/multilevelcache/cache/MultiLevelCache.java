package com.slm.multilevelcache.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.slm.multilevelcache.config.CacheProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 多级缓存
 *
 * <p>读取顺序：L1(Caffeine) → L2(Redis) → DB；写入同时写 L1 与 L2；删除同时删 L1 与 L2。</p>
 *
 * <p>L1 是节点私有的，删除只能删到本节点，因此跨节点的一致性由
 * {@link com.slm.multilevelcache.consistency.CacheConsistencyService} 通过
 * Redis 广播失效消息来解决，本类只负责单节点内的层级读写。</p>
 */
@Slf4j
public class MultiLevelCache extends AbstractValueAdaptingCache {

    private final String name;

    /**
     * L1：本地缓存，进程内
     */
    private final Cache<Object, Object> localCache;

    /**
     * L2：分布式缓存
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * L2 key 前缀，多缓存共用一个 Redis 实例时靠前缀隔离
     */
    private final String keyPrefix;

    private final CacheProperties properties;

    public MultiLevelCache(String name,
                           Cache<Object, Object> localCache,
                           RedisTemplate<String, Object> redisTemplate,
                           CacheProperties properties) {
        // 允许缓存空值，由 CacheProperties.cacheNullValue 统一控制
        super(properties.isCacheNullValue());
        this.name = name;
        this.localCache = localCache;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.keyPrefix = "multi-level-cache:" + name + ":";
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this.localCache;
    }

    /**
     * 读取：先查 L1，未命中再查 L2，命中 L2 时回填 L1。
     *
     * <p>缓存异常时降级为「未命中」，让请求继续回源数据库：
     * 缓存是加速器，不应该成为可用性单点。</p>
     */
    @Override
    public ValueWrapper get(Object key) {
        Object k = toStoreKey(key);

        // L1
        try {
            Object local = this.localCache.getIfPresent(k);
            if (local != null) {
                return new SimpleValueWrapper(local);
            }
        } catch (Exception e) {
            log.warn("[{}] 读取 L1 本地缓存失败，降级查 L2：{}", name, e.getMessage());
        }

        // L2
        try {
            Object remote = this.redisTemplate.opsForValue().get(remoteKey(k));
            if (remote != null) {
                // 回填 L1，下次读走纳秒级本地缓存
                this.localCache.put(k, remote);
                return new SimpleValueWrapper(remote);
            }
        } catch (Exception e) {
            log.warn("[{}] 读取 L2 分布式缓存失败，降级回源：{}", name, e.getMessage());
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object value = get(key).get();
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                    "Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    /**
     * 加载并回填。仅在「L1 与 L2 都没有」时被调用。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return (T) existing.get();
        }
        try {
            T value = valueLoader.call();
            if (value != null) {
                put(key, value);
            }
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    /**
     * 写入：先写 L2 再写 L1。
     *
     * <p>顺序是有意的：L1 的写入无失败可能，先落 L2 可以保证「只要 L1 有值，L2 一定也有」，
     * 从而避免广播消息晚于本地写入到达时出现的不一致。</p>
     */
    @Override
    public void put(Object key, Object value) {
        Object k = toStoreKey(key);
        Duration ttl = properties.isCacheNullValue() && value instanceof CacheNullValue
                ? properties.getNullValueTtl()
                : properties.getRemoteTtl();

        try {
            this.redisTemplate.opsForValue().set(remoteKey(k), value, ttlWithJitter(ttl));
        } catch (Exception e) {
            // L2 写失败不能影响主流程，本地缓存仍然生效，只是下次回源要多打一次数据库
            log.warn("[{}] 写入 L2 分布式缓存失败：{}", name, e.getMessage());
        }
        this.localCache.put(k, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    /**
     * 删除：同时清 L1 与 L2
     */
    @Override
    public void evict(Object key) {
        evictLocal(key);
        evictRemote(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean present = get(key) != null;
        evict(key);
        return present;
    }

    /**
     * 清空：仅清理本缓存名下的 key。
     *
     * <p>L2 使用 KEPS 模式匹配，生产环境请改用 SCAN 游标遍历，
     * 否则大 keyspace 下会阻塞 Redis 主线程。</p>
     */
    @Override
    public void clear() {
        evictAllLocal();
        Set<String> keys = this.redisTemplate.keys(keyPrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            this.redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean invalidate() {
        clear();
        return true;
    }

    /**
     * 供 {@link AbstractValueAdaptingCache} 使用的原始值读取
     */
    @Override
    protected Object lookup(Object key) {
        return get(key).get();
    }

    /**
     * 仅清理本节点 L1，不触碰 L2。
     *
     * <p>这是广播失效消息的落点：收到其他节点发来的失效通知时只清本地，
     * 因为发起方已经删过 L2 了。</p>
     */
    public void evictLocal(Object key) {
        this.localCache.invalidate(toStoreKey(key));
    }

    /**
     * 仅清空本节点 L1，不触碰 L2
     */
    public void evictAllLocal() {
        this.localCache.invalidateAll();
    }

    /**
     * 仅清理 L2
     */
    public void evictRemote(Object key) {
        try {
            this.redisTemplate.delete(remoteKey(toStoreKey(key)));
        } catch (Exception e) {
            log.warn("[{}] 删除 L2 分布式缓存失败：{}", name, e.getMessage());
        }
    }

    /**
     * 带层级信息的读取，供埋点与测试断言使用。
     *
     * <p>返回值中的 {@code level} 区分了到底是 L1 还是 L2 命中，
     * 二者性能差距很大，只看「是否命中」无法评估本地缓存的收益。</p>
     *
     * @return 未命中返回 null
     */
    public LevelValue getWithLevel(Object key) {
        Object k = toStoreKey(key);

        try {
            Object local = this.localCache.getIfPresent(k);
            if (local != null) {
                return new LevelValue(local, CacheLevel.L1_CAFFEINE);
            }
        } catch (Exception e) {
            log.warn("[{}] 读取 L1 本地缓存失败，降级查 L2：{}", name, e.getMessage());
        }

        try {
            Object remote = this.redisTemplate.opsForValue().get(remoteKey(k));
            if (remote != null) {
                this.localCache.put(k, remote);
                return new LevelValue(remote, CacheLevel.L2_REDIS);
            }
        } catch (Exception e) {
            log.warn("[{}] 读取 L2 分布式缓存失败，降级回源：{}", name, e.getMessage());
        }

        return null;
    }

    /**
     * 带层级标识的缓存值
     */
    @Getter
    @AllArgsConstructor
    public static class LevelValue {

        private final Object value;

        private final CacheLevel level;

    }

    /**
     * 一致性对账：把 L1 中在 L2 已经不存在的条目清掉。
     *
     * <p>覆盖的场景：Pub/Sub 广播丢失、节点在消息到达前重启、或 L2 因故提前过期。
     * 这些情况下 L1 会残留数据库中早已变更的旧值，直到本地 TTL 到期才自然消失。
     * 对账任务把它提前收敛掉。</p>
     */
    public void reconcileLocalAgainstRemote() {
        int stale = 0;
        for (Object key : this.localCache.asMap().keySet()) {
            try {
                Boolean exists = this.redisTemplate.hasKey(remoteKey(key));
                if (Boolean.FALSE.equals(exists)) {
                    this.localCache.invalidate(key);
                    stale++;
                }
            } catch (Exception e) {
                // Redis 不可用时不做任何清理，避免把好数据误删
                log.warn("[{}] 一致性对账访问 L2 失败，本轮跳过：{}", name, e.getMessage());
                return;
            }
        }
        if (stale > 0) {
            log.info("[{}] 一致性对账清理了 {} 条 L1 陈旧数据", name, stale);
        }
    }

    /**
     * 判断某个 key 当前是否存在于本节点 L1
     */
    public boolean containsLocal(Object key) {
        return this.localCache.getIfPresent(toStoreKey(key)) != null;
    }

    /**
     * L2 存活时间叠加抖动，打散到期时间
     */
    private Duration ttlWithJitter(Duration ttl) {
        double jitter = properties.getRemoteTtlJitter();
        if (jitter <= 0 || ttl.isZero() || ttl.isNegative()) {
            return ttl;
        }
        long base = ttl.toMillis();
        long range = (long) (base * jitter);
        long offset = range <= 0 ? 0 : ThreadLocalRandom.current().nextLong(-range, range + 1);
        return Duration.ofMillis(Math.max(1_000L, base + offset));
    }

    private String remoteKey(Object key) {
        return keyPrefix + key;
    }

    /**
     * 统一 key 序列化：避免不同调用方传入 Long / "1" 时落到不同的缓存条目
     */
    private Object toStoreKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Cache key must not be null");
        }
        return key instanceof String ? key : String.valueOf(key);
    }

    /**
     * 供测试与排查使用的 L1 统计
     */
    public long localSize() {
        return this.localCache.estimatedSize();
    }

}