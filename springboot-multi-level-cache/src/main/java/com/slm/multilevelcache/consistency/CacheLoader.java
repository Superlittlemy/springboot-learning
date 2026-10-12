package com.slm.multilevelcache.consistency;

import com.slm.multilevelcache.cache.CacheLevel;
import com.slm.multilevelcache.cache.CacheNullValue;
import com.slm.multilevelcache.cache.MultiLevelCache;
import com.slm.multilevelcache.config.CacheProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 缓存加载器
 *
 * <p>负责把「三级读取 + 防击穿 + 防穿透 + 命中埋点」串成一条完整的读路径。
 * 业务侧只需调用 {@link #load}，不必关心多级缓存的细节。</p>
 *
 * <p>防击穿策略：热点 key 失效后，抢到锁的线程去回源数据库并回填，
 * 没抢到的线程直接返回旧值（如果有）或快速失败，绝不一起去打数据库。</p>
 */
@Slf4j
@Component
public class CacheLoader {

    private final DistributedLock distributedLock;

    private final CacheProperties properties;

    public CacheLoader(DistributedLock distributedLock, CacheProperties properties) {
        this.distributedLock = distributedLock;
        this.properties = properties;
    }

    /**
     * 按 L1 → L2 → 回源 的顺序加载
     *
     * @param cache    目标缓存
     * @param key      缓存 key
     * @param fallback 回源到数据库的逻辑，返回 null 表示数据不存在
     * @return 加载结果，含数据与实际命中的层级
     */
    public <T> CacheResult<T> load(Cache cache, Object key, Callable<T> fallback) {
        CacheResult<T> hit = tryRead(cache, key);
        if (hit.isHit()) {
            return hit;
        }

        // 缓存未命中：加锁回源，避免并发请求同时穿透到数据库
        String lockKey = cache.getName() + ":" + key;
        String token = newToken();
        boolean locked = false;
        try {
            locked = distributedLock.tryLock(lockKey, token);

            if (locked) {
                // 抢到锁：再查一次缓存，避免等锁期间别人已经回填好了，白跑一趟数据库
                CacheResult<T> second = tryRead(cache, key);
                if (second.isHit()) {
                    return second;
                }
                return doLoad(cache, key, fallback);
            }

            // 没抢到锁：缓存正在被其它线程重建。
            // 回源结果还没写回缓存，此刻数据库已有新数据，直接回源比空等更可靠，
            // 但这里选择短暂等待后再读缓存，避免把并发放大到数据库。
            Thread.sleep(50L);
            CacheResult<T> afterWait = tryRead(cache, key);
            if (afterWait.isHit()) {
                return afterWait;
            }
            // 等待后仍未回填，自己兜底回源，保证语义正确
            return doLoad(cache, key, fallback);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CacheResult<>(null, CacheLevel.DB, false);
        } catch (Exception e) {
            log.warn("缓存加载失败，回源数据库：{}", e.getMessage());
            return new CacheResult<>(null, CacheLevel.DB, false);
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey, token);
            }
        }
    }

    /**
     * 回源数据库并回填缓存
     */
    private <T> CacheResult<T> doLoad(Cache cache, Object key, Callable<T> fallback) throws Exception {
        T value = fallback.call();

        if (value == null) {
            // 防穿透：查不到也写一个短 TTL 的空值标记，避免不存在的 key 反复回源
            if (properties.isCacheNullValue()) {
                cache.put(key, CacheNullValue.INSTANCE);
            }
            return new CacheResult<>(null, CacheLevel.NULL, false);
        }

        cache.put(key, value);
        // 回源数据库不算缓存命中，命中率为统计口径时不能计入
        return new CacheResult<>(value, CacheLevel.DB, false);
    }

    /**
     * 依次读 L1 与 L2，并把空值标记翻译成真正的 null
     */
    @SuppressWarnings("unchecked")
    private <T> CacheResult<T> tryRead(Cache cache, Object key) {
        // 优先用带层级信息的读取，避免把 L2 命中误报成 L1 命中
        if (cache instanceof MultiLevelCache) {
            MultiLevelCache.LevelValue levelValue = ((MultiLevelCache) cache).getWithLevel(key);
            if (levelValue == null) {
                return CacheResult.miss();
            }
            Object value = levelValue.getValue();
            if (value instanceof CacheNullValue) {
                return new CacheResult<>(null, CacheLevel.NULL, true);
            }
            return new CacheResult<>((T) value, levelValue.getLevel(), true);
        }

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) {
            return CacheResult.miss();
        }
        Object value = wrapper.get();
        if (value instanceof CacheNullValue) {
            return new CacheResult<>(null, CacheLevel.NULL, true);
        }
        return new CacheResult<>((T) value, CacheLevel.L1_CAFFEINE, true);
    }

    private String newToken() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 加载结果
     *
     * @param value 数据；未命中且不存在时为 null
     * @param level 实际命中的层级，用于响应头埋点
     * @param hit   是否从缓存命中（用于统计缓存命中率）
     */
    @Getter
    public static class CacheResult<T> {

        private final T value;

        private final CacheLevel level;

        private final boolean hit;

        public CacheResult(T value, CacheLevel level, boolean hit) {
            this.value = value;
            this.level = level;
            this.hit = hit;
        }

        public static <T> CacheResult<T> miss() {
            return new CacheResult<>(null, CacheLevel.DB, false);
        }

        public boolean isPresent() {
            return value != null;
        }
    }

}