package com.slm.multilevelcache.consistency;

import com.slm.multilevelcache.cache.CacheLevel;
import com.slm.multilevelcache.cache.CacheNullValue;
import com.slm.multilevelcache.cache.MultiLevelCache;
import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.config.CacheProperties;
import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 缓存加载器测试
 *
 * <p>覆盖三级读取顺序、防穿透空值缓存、防击穿互斥重建。</p>
 */
class CacheLoaderTest {

    private FakeRedis redis;

    private CacheProperties properties;

    private MultiLevelCacheManager cacheManager;

    /**
     * 永远抢不到锁的替身，用于模拟别的线程正在重建缓存
     */
    private DistributedLock lockDenied;

    /**
     * 总是抢到锁的替身
     */
    private DistributedLock lockGranted;

    private CacheLoader loader;

    @BeforeEach
    void setUp() {
        this.redis = new FakeRedis();
        this.properties = FakeRedis.properties();
        this.cacheManager = new MultiLevelCacheManager(this.redis.template(), this.properties);

        this.lockDenied = mock(DistributedLock.class);
        when(this.lockDenied.tryLock(anyString(), anyString())).thenReturn(false);

        this.lockGranted = mock(DistributedLock.class);
        when(this.lockGranted.tryLock(anyString(), anyString())).thenReturn(true);

        this.loader = new CacheLoader(this.lockGranted, this.properties);
    }

    @Test
    @DisplayName("L1 命中时不应访问数据库")
    void shouldHitL1WithoutTouchingDb() {
        Cache cache = this.cacheManager.getCache("product");
        cache.put("1001", "机械键盘");
        AtomicInteger dbCalls = new AtomicInteger();

        CacheLoader.CacheResult<String> result = this.loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        assertThat(result.isHit()).isTrue();
        assertThat(result.getLevel()).isEqualTo(CacheLevel.L1_CAFFEINE);
        assertThat(result.getValue()).isEqualTo("机械键盘");
        assertThat(dbCalls.get()).as("L1 命中不应回源").isZero();
    }

    @Test
    @DisplayName("L2 命中时回源次数为 0，且层级标记为 L2")
    void shouldHitL2WithoutTouchingDb() {
        Cache cache = this.cacheManager.getCache("product");
        this.redis.rawPut(FakeRedis.redisKey("product", "1001"), "L2 的值");
        AtomicInteger dbCalls = new AtomicInteger();

        CacheLoader.CacheResult<String> result = this.loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        assertThat(result.isHit()).isTrue();
        assertThat(result.getLevel()).isEqualTo(CacheLevel.L2_REDIS);
        assertThat(result.getValue()).isEqualTo("L2 的值");
        assertThat(dbCalls.get()).isZero();
    }

    @Test
    @DisplayName("全部未命中时回源数据库并回填缓存")
    void shouldFallbackToDbAndPopulate() {
        Cache cache = this.cacheManager.getCache("product");
        AtomicInteger dbCalls = new AtomicInteger();

        CacheLoader.CacheResult<String> first = this.loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        assertThat(first.isHit()).as("回源数据库不算缓存命中").isFalse();
        assertThat(first.getLevel()).isEqualTo(CacheLevel.DB);
        assertThat(first.getValue()).isEqualTo("数据库值");
        assertThat(dbCalls.get()).isEqualTo(1);

        // 回填后第二次应命中 L1，不再回源
        CacheLoader.CacheResult<String> second = this.loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        assertThat(second.getLevel()).isEqualTo(CacheLevel.L1_CAFFEINE);
        assertThat(dbCalls.get()).as("命中缓存后不应重复回源").isEqualTo(1);
    }

    @Test
    @DisplayName("防穿透：查不到数据时应写入空值占位，二次请求不再回源")
    void shouldCacheNullValueToPreventPenetration() {
        Cache cache = this.cacheManager.getCache("product");
        AtomicInteger dbCalls = new AtomicInteger();

        CacheLoader.CacheResult<String> first = this.loader.load(cache, "9999", () -> {
            dbCalls.incrementAndGet();
            return null;
        });

        assertThat(first.isPresent()).isFalse();
        assertThat(first.getLevel()).isEqualTo(CacheLevel.NULL);
        assertThat(this.redis.rawGet(FakeRedis.redisKey("product", "9999")))
                .as("应写入空值占位符").isEqualTo(CacheNullValue.INSTANCE);

        CacheLoader.CacheResult<String> second = this.loader.load(cache, "9999", () -> {
            dbCalls.incrementAndGet();
            return null;
        });

        assertThat(second.getValue()).isNull();
        assertThat(second.getLevel()).isEqualTo(CacheLevel.NULL);
        assertThat(dbCalls.get()).as("命中空值占位后不应再次回源").isEqualTo(1);
    }

    @Test
    @DisplayName("关闭空值缓存时不写占位符，允许每次回源")
    void shouldNotCacheNullWhenDisabled() {
        this.properties.setCacheNullValue(false);
        this.loader = new CacheLoader(this.lockGranted, this.properties);
        Cache cache = this.cacheManager.getCache("product");

        CacheLoader.CacheResult<String> result = this.loader.load(cache, "9999", () -> null);

        assertThat(result.isPresent()).isFalse();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "9999")))
                .as("未开启时不应写入占位符").isFalse();
    }

    @Test
    @DisplayName("防击穿：抢到锁的线程负责回源并释放锁")
    void shouldHoldLockWhileRebuilding() {
        CacheLoader loader = new CacheLoader(this.lockGranted, this.properties);
        Cache cache = this.cacheManager.getCache("product");

        loader.load(cache, "1001", () -> "数据库值");

        org.mockito.Mockito.verify(this.lockGranted).tryLock(anyString(), anyString());
        org.mockito.Mockito.verify(this.lockGranted).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("防击穿：抢锁失败时应先等待重读，等待后仍未回填则兜底回源")
    void shouldFallbackToDbWhenLockContended() {
        CacheLoader loader = new CacheLoader(this.lockDenied, this.properties);
        Cache cache = this.cacheManager.getCache("product");
        AtomicInteger dbCalls = new AtomicInteger();

        CacheLoader.CacheResult<String> result = loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        // 抢不到锁说明别的线程正在重建，等待后缓存仍为空。
        // 此时必须兜底回源：返回空会让「数据存在」被误判成「数据不存在」，
        // 缓存绝不能改变查询结果。多一次回源只是多一次数据库压力，语义正确优先。
        assertThat(dbCalls.get()).as("等待后仍未回填应兜底回源").isEqualTo(1);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.getValue()).isEqualTo("数据库值");
    }

    @Test
    @DisplayName("防击穿：抢锁失败但等待期间别的线程已回填时，不应再回源")
    void shouldNotFallbackWhenFilledByOtherThreadWhileWaiting() {
        Cache cache = this.cacheManager.getCache("product");
        AtomicInteger dbCalls = new AtomicInteger();

        // 模拟：等待期间另一个线程把值回填进了缓存
        DistributedLock warmingLock = new DistributedLock() {
            @Override
            public boolean tryLock(String lockKey, String token) {
                cache.put("1001", "别的线程回填的值");
                return false;
            }

            @Override
            public void unlock(String lockKey, String token) {
            }
        };
        CacheLoader loader = new CacheLoader(warmingLock, this.properties);

        CacheLoader.CacheResult<String> result = loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        assertThat(dbCalls.get()).as("等待后命中缓存就不应回源").isZero();
        assertThat(result.getValue()).isEqualTo("别的线程回填的值");
    }

    @Test
    @DisplayName("回源抛异常时应降级，不向上抛")
    void shouldDegradeWhenFallbackThrows() {
        CacheLoader loader = new CacheLoader(this.lockGranted, this.properties);
        Cache cache = this.cacheManager.getCache("product");

        CacheLoader.CacheResult<String> result = loader.load(cache, "1001", () -> {
            throw new IllegalStateException("数据库不可用");
        });

        assertThat(result.isPresent()).isFalse();
        assertThat(result.isHit()).isFalse();
    }

    @Test
    @DisplayName("拿到锁后应二次检查缓存，避免重复回源")
    void shouldRecheckCacheAfterAcquiringLock() {
        Cache cache = this.cacheManager.getCache("product");
        AtomicInteger dbCalls = new AtomicInteger();

        // 模拟：等待锁期间，另一个线程已经把值回填好了
        DistributedLock slowLock = new DistributedLock() {
            @Override
            public boolean tryLock(String lockKey, String token) {
                cache.put("1001", "别的线程回填的值");
                return true;
            }

            @Override
            public void unlock(String lockKey, String token) {
            }
        };
        CacheLoader loader = new CacheLoader(slowLock, this.properties);

        CacheLoader.CacheResult<String> result = loader.load(cache, "1001", () -> {
            dbCalls.incrementAndGet();
            return "数据库值";
        });

        assertThat(dbCalls.get()).as("二次检查命中后不应回源").isZero();
        assertThat(result.getValue()).isEqualTo("别的线程回填的值");
    }

    @Test
    @DisplayName("缓存不可用时也应能完成加载")
    void shouldStillWorkWithNonMultiLevelCache() {
        // 用一个普通 Spring Cache 验证降级路径不会崩
        org.springframework.cache.concurrent.ConcurrentMapCache plain =
                new org.springframework.cache.concurrent.ConcurrentMapCache("plain");
        plain.put("1001", "普通缓存值");

        CacheLoader.CacheResult<String> hit = this.loader.load(plain, "1001", () -> "数据库值");
        assertThat(hit.getValue()).isEqualTo("普通缓存值");

        CacheLoader.CacheResult<String> miss = this.loader.load(plain, "1002", () -> "数据库值");
        assertThat(miss.getValue()).isEqualTo("数据库值");
    }

    @Test
    @DisplayName("MultiLevelCache 的 containsLocal 应反映 L1 状态")
    void multiLevelCacheShouldExposeLocalState() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "机械键盘");

        assertThat(cache.containsLocal("1001")).isTrue();
        assertThat(cache.localSize()).isEqualTo(1);
    }

}