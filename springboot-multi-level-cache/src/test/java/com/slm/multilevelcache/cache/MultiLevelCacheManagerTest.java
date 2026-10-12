package com.slm.multilevelcache.cache;

import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多级缓存管理器测试
 *
 * <p>重点验证缓存实例的唯一性——广播失效消息要能准确找到持有数据的那个 L1 实例。</p>
 */
class MultiLevelCacheManagerTest {

    private FakeRedis redis;

    private MultiLevelCacheManager manager;

    @BeforeEach
    void setUp() {
        this.redis = new FakeRedis();
        this.manager = new MultiLevelCacheManager(this.redis.template(), FakeRedis.properties());
    }

    @Test
    @DisplayName("同名缓存应返回同一个实例")
    void shouldReturnSameInstanceForSameName() {
        Cache first = this.manager.getCache("product");
        Cache second = this.manager.getCache("product");

        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("不同名缓存应是不同实例，且缓存名可枚举")
    void shouldIsolateCacheNames() {
        Cache product = this.manager.getCache("product");
        Cache productList = this.manager.getCache("product:list");

        assertThat(product).isNotSameAs(productList);
        assertThat(this.manager.getCacheNames()).contains("product", "product:list");
        assertThat(this.manager.getCaches()).hasSize(2);
    }

    @Test
    @DisplayName("evictLocal 只清本节点 L1，不碰 L2")
    void evictLocalShouldNotTouchRemote() {
        MultiLevelCache cache = (MultiLevelCache) this.manager.getCache("product");
        cache.put("1001", "机械键盘");

        this.manager.evictLocal("product", "1001");

        assertThat(cache.containsLocal("1001")).isFalse();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "1001")))
                .as("广播场景下 L2 由发起方负责删除").isTrue();
    }

    @Test
    @DisplayName("对未知缓存名做失效广播应安全忽略")
    void evictLocalShouldIgnoreUnknownCache() {
        this.manager.evictLocal("never-used", "1001");

        assertThat(this.manager.getCacheNames()).isEmpty();
    }

    @Test
    @DisplayName("clearLocal 只清空指定缓存的 L1")
    void clearLocalShouldOnlyClearGivenCache() {
        MultiLevelCache product = (MultiLevelCache) this.manager.getCache("product");
        MultiLevelCache list = (MultiLevelCache) this.manager.getCache("product:list");
        product.put("1001", "机械键盘");
        list.put("键盘", "查询结果");

        this.manager.clearLocal("product");

        assertThat(product.localSize()).isZero();
        assertThat(list.localSize()).as("其它缓存的 L1 不受影响").isEqualTo(1);
    }

    @Test
    @DisplayName("reconcile 应汇总清理掉的陈旧条目数")
    void reconcileShouldReturnCleanedCount() {
        MultiLevelCache product = (MultiLevelCache) this.manager.getCache("product");
        product.put("1001", "机械键盘");
        product.put("1002", "人体工学椅");
        // 两个 key 同时从 L2 消失，模拟广播全部丢失
        this.redis.store().remove(FakeRedis.redisKey("product", "1001"));
        this.redis.store().remove(FakeRedis.redisKey("product", "1002"));

        int cleaned = this.manager.reconcile();

        assertThat(cleaned).isEqualTo(2);
        assertThat(product.localSize()).isZero();
    }

    @Test
    @DisplayName("L2 正常时对账不应删除任何数据")
    void reconcileShouldBeNoopWhenConsistent() {
        MultiLevelCache product = (MultiLevelCache) this.manager.getCache("product");
        product.put("1001", "机械键盘");

        int cleaned = this.manager.reconcile();

        assertThat(cleaned).isZero();
        assertThat(product.containsLocal("1001")).isTrue();
    }

}