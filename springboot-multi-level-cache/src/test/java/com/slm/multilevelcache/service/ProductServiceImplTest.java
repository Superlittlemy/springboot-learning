package com.slm.multilevelcache.service;

import com.slm.multilevelcache.cache.CacheLevel;
import com.slm.multilevelcache.cache.MultiLevelCache;
import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.config.CacheProperties;
import com.slm.multilevelcache.consistency.CacheConsistencyService;
import com.slm.multilevelcache.consistency.CacheLoader;
import com.slm.multilevelcache.consistency.DistributedLock;
import com.slm.multilevelcache.entity.Product;
import com.slm.multilevelcache.enums.ResultStatus;
import com.slm.multilevelcache.exception.CacheException;
import com.slm.multilevelcache.repository.InMemoryProductRepository;
import com.slm.multilevelcache.service.impl.ProductServiceImpl;
import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品服务测试
 *
 * <p>重点验证写路径的顺序契约：先更新数据库，再删除缓存。</p>
 */
class ProductServiceImplTest {

    private InMemoryProductRepository repository;

    private FakeRedis redis;

    private MultiLevelCacheManager cacheManager;

    private CacheProperties properties;

    private ProductServiceImpl service;

    private CacheConsistencyService consistencyService;

    @BeforeEach
    void setUp() {
        this.repository = new InMemoryProductRepository();
        this.redis = new FakeRedis();
        this.properties = FakeRedis.properties();
        this.cacheManager = new MultiLevelCacheManager(this.redis.template(), this.properties);

        DistributedLock lock = mock(DistributedLock.class);
        when(lock.tryLock(anyString(), anyString())).thenReturn(true);
        CacheLoader cacheLoader = new CacheLoader(lock, this.properties);

        this.consistencyService = new CacheConsistencyService();
        ReflectionTestUtils.setField(this.consistencyService, "redisTemplate", this.redis.template());
        ReflectionTestUtils.setField(this.consistencyService, "cacheManager", this.cacheManager);
        ReflectionTestUtils.setField(this.consistencyService, "properties", this.properties);
        // 延迟双删用 mock 调度器即可：这里只关心失效逻辑，不需要真的等 1 秒
        ReflectionTestUtils.setField(this.consistencyService, "taskScheduler",
                org.mockito.Mockito.mock(org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler.class));
        ReflectionTestUtils.setField(this.consistencyService, "nodeId", "test-node");

        this.service = new ProductServiceImpl();
        ReflectionTestUtils.setField(this.service, "productRepository", this.repository);
        ReflectionTestUtils.setField(this.service, "cacheManager", this.cacheManager);
        ReflectionTestUtils.setField(this.service, "cacheLoader", cacheLoader);
        ReflectionTestUtils.setField(this.service, "cacheConsistencyService", this.consistencyService);
    }

    @Test
    @DisplayName("首次查询回源数据库并回填缓存")
    void firstReadShouldFallbackToRepository() {
        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();

        CacheLoader.CacheResult<Product> result = this.service.getById(id);

        assertThat(result.isPresent()).isTrue();
        assertThat(result.getLevel()).isEqualTo(CacheLevel.DB);
        assertThat(result.getValue().getName()).isEqualTo("机械键盘");
    }

    @Test
    @DisplayName("二次查询命中 L1，不再回源")
    void secondReadShouldHitLocalCache() {
        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();
        this.service.getById(id);

        CacheLoader.CacheResult<Product> result = this.service.getById(id);

        assertThat(result.getLevel()).isEqualTo(CacheLevel.L1_CAFFEINE);
    }

    @Test
    @DisplayName("查询不存在的商品应返回空结果而非异常")
    void shouldReturnEmptyForMissingProduct() {
        CacheLoader.CacheResult<Product> result = this.service.getById(99999L);

        assertThat(result.isPresent()).isFalse();
        assertThat(result.getLevel()).isEqualTo(CacheLevel.NULL);
    }

    @Test
    @DisplayName("id 为 null 时应抛出业务异常")
    void shouldRejectNullId() {
        assertThatThrownBy(() -> this.service.getById(null))
                .isInstanceOf(CacheException.class)
                .extracting(e -> ((CacheException) e).getStatus())
                .isEqualTo(ResultStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("更新后应清理详情缓存，下次读取能拿到新值")
    void updateShouldEvictDetailCache() {
        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();
        this.service.getById(id);

        Product update = new Product();
        update.setId(id);
        update.setName("机械键盘");
        update.setPrice(new BigDecimal("599.00"));
        update.setStock(50);
        this.service.update(update);

        CacheLoader.CacheResult<Product> result = this.service.getById(id);

        assertThat(result.getValue().getPrice()).isEqualTo(new BigDecimal("599.00"));
    }

    @Test
    @DisplayName("更新不存在的商品应抛出业务异常")
    void updateMissingProductShouldThrow() {
        Product update = new Product();
        update.setId(99999L);
        update.setName("不存在的商品");

        assertThatThrownBy(() -> this.service.update(update))
                .isInstanceOf(CacheException.class)
                .extracting(e -> ((CacheException) e).getStatus())
                .isEqualTo(ResultStatus.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("写操作必须先落库再删缓存")
    void updateShouldWriteRepositoryBeforeEvicting() {
        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();
        this.service.getById(id);

        Product update = new Product();
        update.setId(id);
        update.setName("机械键盘 Pro");
        update.setPrice(new BigDecimal("1299.00"));

        this.service.update(update);

        // 数据库应已是新值，且缓存已被清理，两者顺序不能颠倒
        assertThat(this.repository.findById(id)).isPresent();
        assertThat(this.repository.findById(id).get().getPrice()).isEqualTo(new BigDecimal("1299.00"));
        MultiLevelCache detail = (MultiLevelCache) this.cacheManager.getCache(ProductServiceImpl.CACHE_PRODUCT);
        assertThat(detail.containsLocal(id)).as("详情缓存应被清理").isFalse();
    }

    @Test
    @DisplayName("更新商品应整体清理列表缓存")
    void updateShouldEvictListCache() {
        this.service.listByName("键盘");

        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();
        Product update = new Product();
        update.setId(id);
        update.setName("机械键盘");

        this.service.update(update);

        MultiLevelCache list = (MultiLevelCache) this.cacheManager.getCache(ProductServiceImpl.CACHE_PRODUCT_LIST);
        assertThat(list.localSize()).as("列表缓存应被清理").isZero();
    }

    @Test
    @DisplayName("删除后详情缓存应失效，且再次查询返回空")
    void deleteShouldEvictAndRemove() {
        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();
        this.service.getById(id);

        assertThat(this.service.delete(id)).isTrue();

        assertThat(this.repository.findById(id)).isEmpty();
        assertThat(this.service.getById(id).isPresent()).isFalse();
    }

    @Test
    @DisplayName("删除不存在的商品返回 false，不应抛异常")
    void deleteMissingShouldReturnFalse() {
        assertThat(this.service.delete(99999L)).isFalse();
    }

    @Test
    @DisplayName("按名称查询应走缓存并返回列表")
    void listByNameShouldUseCache() {
        List<Product> first = this.service.listByName("键盘");
        List<Product> second = this.service.listByName("键盘");

        assertThat(first).isNotEmpty();
        assertThat(second).hasSameSizeAs(first);
    }

    @Test
    @DisplayName("关键词为空时查询全部商品")
    void listWithNoKeywordShouldReturnAll() {
        List<Product> all = this.service.listByName(null);

        assertThat(all).hasSize(this.repository.count());
    }

    @Test
    @DisplayName("写路径应通过一致性服务广播失效")
    void updateShouldBroadcastInvalidation() {
        Long id = this.repository.findByNameContaining("机械键盘").get(0).getId();
        this.service.getById(id);

        Product update = new Product();
        update.setId(id);
        update.setName("机械键盘");
        this.service.update(update);

        // 更新会触发详情与列表两次失效，因此广播不止一次
        verify(this.redis.template(), org.mockito.Mockito.atLeastOnce())
                .convertAndSend(anyString(), org.mockito.ArgumentMatchers.any());
    }

}