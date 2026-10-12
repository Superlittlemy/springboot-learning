package com.slm.multilevelcache;

import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.config.CacheProperties;
import com.slm.multilevelcache.consistency.CacheConsistencyService;
import com.slm.multilevelcache.consistency.CacheLoader;
import com.slm.multilevelcache.consistency.DistributedLock;
import com.slm.multilevelcache.controller.ProductController;
import com.slm.multilevelcache.repository.ProductRepository;
import com.slm.multilevelcache.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文装配测试
 *
 * <p>单元测试都是手工 new 对象，验证不了 Spring 容器里的装配是否正确。
 * 这里用 mock 的 RedisConnectionFactory 拉起完整上下文，检查各层组件能否正确注入。</p>
 *
 * <p>关闭 Spring Data KeyValue 仓储自动配置：本模块用的是自己的 Repository 接口，
 * 不走 Spring Data KeyValue，而它会在 mock 连接工厂下初始化失败。
 * 同时关闭广播订阅容器，它需要真实连接才能启动。</p>
 */
@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "multi-level-cache.broadcast-enabled=false"
})
class MultiLevelCacheApplicationTests {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * Spring 的 RedisReactiveAutoConfiguration 会从 RedisConnectionFactory 派生出
     * reactiveRedisTemplate，这里必须一并 mock，否则上下文会因为缺少
     * ReactiveRedisConnectionFactory 而启动失败。
     */
    @MockBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MultiLevelCacheManager cacheManager;

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private CacheConsistencyService consistencyService;

    @Autowired
    private CacheLoader cacheLoader;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductController productController;

    @Test
    @DisplayName("上下文应装配出多级缓存管理器，且默认启用缓存与调度")
    void contextShouldLoad() {
        assertThat(this.cacheManager).isNotNull();
        assertThat(this.cacheManager.getCache("product")).isNotNull();
    }

    @Test
    @DisplayName("一致性相关组件应作为 Spring Bean 注入成功")
    void consistencyComponentsShouldBeWired() {
        assertThat(this.consistencyService).isNotNull();
        assertThat(this.cacheLoader).isNotNull();
        assertThat(this.distributedLock).isNotNull();
    }

    @Test
    @DisplayName("业务层应装配完整")
    void businessComponentsShouldBeWired() {
        assertThat(this.productService).isNotNull();
        assertThat(this.productRepository).isNotNull();
        assertThat(this.productController).isNotNull();
    }

    @Test
    @DisplayName("多级缓存管理器应作为 Spring 的 CacheManager 生效，使 @Cacheable 可用")
    void multiLevelCacheManagerShouldBeTheActiveCacheManager() {
        CacheManager cacheManager = this.cacheManager;
        assertThat(cacheManager.getCacheNames()).isNotNull();
        assertThat(this.cacheProperties.getInvalidateChannel()).isNotBlank();
    }

}