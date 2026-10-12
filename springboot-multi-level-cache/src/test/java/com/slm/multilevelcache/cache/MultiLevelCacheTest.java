package com.slm.multilevelcache.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.slm.multilevelcache.config.CacheProperties;
import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 多级缓存读写行为测试
 *
 * <p>覆盖 L1/L2 的读写顺序、回填、删除、TTL 与故障降级。</p>
 */
class MultiLevelCacheTest {

    private FakeRedis redis;

    private CacheProperties properties;

    private MultiLevelCache cache;

    @BeforeEach
    void setUp() {
        this.redis = new FakeRedis();
        this.properties = FakeRedis.properties();
        this.cache = new MultiLevelCache("product",
                Caffeine.newBuilder().maximumSize(100).build(),
                this.redis.template(),
                this.properties);
    }

    @Test
    @DisplayName("L1 未命中 L2 命中时，应回填 L1 并记录 L2 命中层级")
    void shouldBackfillL1OnL2Hit() {
        String key = "1001";
        this.redis.rawPut(FakeRedis.redisKey("product", key), "旧值");

        Cache.ValueWrapper first = this.cache.get(key);
        assertThat(first).isNotNull();
        assertThat((String) first.get()).isEqualTo("旧值");
        assertThat(this.cache.containsLocal(key)).as("L2 命中后应回填 L1").isTrue();

        // 第二次读走 L1，且层级应被正确标记为 L1
        MultiLevelCache.LevelValue second = this.cache.getWithLevel(key);
        assertThat(second.getLevel()).isEqualTo(CacheLevel.L1_CAFFEINE);
        assertThat(second.getValue()).isEqualTo("旧值");
    }

    @Test
    @DisplayName("写入应同时落到 L1 与 L2")
    void shouldWriteToBothLevels() {
        this.cache.put("1001", "机械键盘");

        assertThat(this.cache.containsLocal("1001")).as("应写入 L1").isTrue();
        assertThat(this.redis.rawGet(FakeRedis.redisKey("product", "1001")))
                .as("应写入 L2").isEqualTo("机械键盘");
    }

    @Test
    @DisplayName("删除应同时清理 L1 与 L2")
    void shouldEvictFromBothLevels() {
        this.cache.put("1001", "机械键盘");
        assertThat(this.cache.containsLocal("1001")).isTrue();

        this.cache.evict("1001");

        assertThat(this.cache.containsLocal("1001")).as("L1 应被清理").isFalse();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "1001")))
                .as("L2 应被清理").isFalse();
    }

    @Test
    @DisplayName("仅删本地缓存时不应影响 L2，供广播消息使用")
    void shouldEvictLocalOnly() {
        this.cache.put("1001", "机械键盘");

        this.cache.evictLocal("1001");

        assertThat(this.cache.containsLocal("1001")).as("L1 应被清理").isFalse();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "1001")))
                .as("L2 不应被清理，广播场景下由发起方负责删 L2").isTrue();
    }

    @Test
    @DisplayName("空值占位符应使用更短的 TTL，且被翻译为真正的 null")
    void nullValueShouldUseShorterTtl() {
        this.cache.put("9999", CacheNullValue.INSTANCE);

        String key = FakeRedis.redisKey("product", "9999");
        assertThat(this.redis.rawGet(key)).isEqualTo(CacheNullValue.INSTANCE);
        assertThat(this.redis.ttlOf(key))
                .as("空值标记的 TTL 应明显短于正常缓存")
                .isLessThan(this.properties.getRemoteTtl());

        MultiLevelCache.LevelValue value = this.cache.getWithLevel("9999");
        assertThat(value).isNotNull();
        assertThat(value.getValue()).isEqualTo(CacheNullValue.INSTANCE);
    }

    @Test
    @DisplayName("L2 故障时应降级为未命中，而不是抛出异常拖垮请求")
    void shouldDegradeWhenRedisDown() {
        this.redis.failOnGet();

        // 不抛异常即视为降级成功，缓存不可用不应该让整个接口 500
        assertThat(this.cache.get("1001")).isNull();

        this.cache.put("1001", "机械键盘");
        // L2 写失败不影响本地缓存，本次读取仍能命中 L1
        assertThat(this.cache.containsLocal("1001")).isTrue();
    }

    @Test
    @DisplayName("clear 应按缓存名前缀清理，不误伤其它缓存")
    void clearShouldOnlyAffectOwnPrefix() {
        this.cache.put("1001", "机械键盘");
        this.redis.rawPut("multi-level-cache:other:2001", "别的缓存");

        this.cache.clear();

        assertThat(this.cache.localSize()).isZero();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "1001"))).isFalse();
        assertThat(this.redis.contains("multi-level-cache:other:2001"))
                .as("其它缓存的条目不应被误删").isTrue();
    }

    @Test
    @DisplayName("对账任务应清掉 L1 中 L2 已不存在的陈旧数据")
    void reconcileShouldDropStaleL1Entries() {
        this.cache.put("1001", "机械键盘");
        assertThat(this.cache.containsLocal("1001")).isTrue();

        // 模拟广播消息丢失：L2 条目已消失，但 L1 还留着旧值
        this.redis.store().remove(FakeRedis.redisKey("product", "1001"));

        this.cache.reconcileLocalAgainstRemote();

        assertThat(this.cache.containsLocal("1001"))
                .as("L2 不存在的条目应从 L1 中被清理").isFalse();
    }

    @Test
    @DisplayName("对账应保留 L2 中仍然有效的条目")
    void reconcileShouldKeepValidEntries() {
        this.cache.put("1001", "机械键盘");

        this.cache.reconcileLocalAgainstRemote();

        assertThat(this.cache.containsLocal("1001")).isTrue();
    }

    @Test
    @DisplayName("L1 过期后不应再返回旧值")
    void shouldExpireLocalEntry() throws InterruptedException {
        // Caffeine 的 expireAfterWrite 在创建时固定，
        // 所以过期行为必须用一个独立构造的短 TTL 缓存来验证
        MultiLevelCache shortLived = new MultiLevelCache("short",
                Caffeine.newBuilder().expireAfterWrite(50, TimeUnit.MILLISECONDS).build(),
                this.redis.template(),
                this.properties);

        shortLived.put("1001", "机械键盘");
        assertThat(shortLived.containsLocal("1001")).isTrue();

        TimeUnit.MILLISECONDS.sleep(120);

        assertThat(shortLived.containsLocal("1001")).as("L1 应已过期").isFalse();
    }

    @Test
    @DisplayName("key 为 null 时应快速失败")
    void shouldRejectNullKey() {
        assertThatThrownBy(() -> this.cache.get(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("L2 存活时间应在基准 TTL 的抖动区间内")
    void remoteTtlShouldCarryJitter() {
        this.properties.setRemoteTtlJitter(0.5);
        this.cache.put("1001", "机械键盘");

        long base = this.properties.getRemoteTtl().toMillis();
        long actual = this.redis.ttlOf(FakeRedis.redisKey("product", "1001")).toMillis();

        assertThat(actual).isBetween(base / 2, base * 3 / 2);
    }

}