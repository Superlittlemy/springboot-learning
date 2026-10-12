package com.slm.multilevelcache.consistency;

import com.slm.multilevelcache.cache.MultiLevelCache;
import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.config.CacheProperties;
import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 缓存一致性测试
 *
 * <p>这是本模块最核心的测试，覆盖：删除缓存、失效广播、延迟双删、广播消息处理。</p>
 */
class CacheConsistencyServiceTest {

    private FakeRedis redis;

    private CacheProperties properties;

    private MultiLevelCacheManager cacheManager;

    private ThreadPoolTaskScheduler scheduler;

    private CacheConsistencyService service;

    @BeforeEach
    void setUp() {
        this.redis = new FakeRedis();
        this.properties = FakeRedis.properties();
        this.cacheManager = new MultiLevelCacheManager(this.redis.template(), this.properties);
        this.scheduler = mock(ThreadPoolTaskScheduler.class);

        this.service = new CacheConsistencyService();
        ReflectionTestUtils.setField(this.service, "redisTemplate", this.redis.template());
        ReflectionTestUtils.setField(this.service, "cacheManager", this.cacheManager);
        ReflectionTestUtils.setField(this.service, "properties", this.properties);
        ReflectionTestUtils.setField(this.service, "taskScheduler", this.scheduler);
        ReflectionTestUtils.setField(this.service, "nodeId", "test-node");
    }

    @Test
    @DisplayName("evict 应同时清理 L1 与 L2，并发布失效广播")
    void evictShouldCleanBothLevelsAndBroadcast() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");
        assertThat(cache.containsLocal("1001")).isTrue();

        this.service.evict("product", "1001");

        assertThat(cache.containsLocal("1001")).as("本节点 L1 应被清理").isFalse();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "1001")))
                .as("L2 应被清理").isFalse();
        verify(this.redis.template()).convertAndSend(
                org.mockito.ArgumentMatchers.eq(this.properties.getInvalidateChannel()),
                ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("延迟双删：应立即删一次，再排定一次延迟删除")
    void shouldScheduleSecondEvictWhenDelayedDoubleDeleteEnabled() {
        this.properties.setDelayedDoubleDeleteEnabled(true);
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        this.service.evict("product", "1001");

        // 第一次已经删掉
        assertThat(cache.containsLocal("1001")).isFalse();

        // 二次删除被排定到调度器上
        verify(this.scheduler, times(1)).schedule(any(Runnable.class), any(Date.class));

        // 模拟并发读线程在两次删除之间把旧值写回了缓存
        cache.put("1001", "旧值");
        assertThat(cache.containsLocal("1001")).as("复现回填旧值的竞态").isTrue();

        // 执行延迟任务后，旧值应被再次清掉
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(this.scheduler).schedule(captor.capture(), any(Date.class));
        captor.getValue().run();

        assertThat(cache.containsLocal("1001")).as("延迟双删应清掉回填的旧值").isFalse();
    }

    @Test
    @DisplayName("关闭延迟双删时不应再排定延迟任务")
    void shouldNotScheduleWhenDoubleDeleteDisabled() {
        this.properties.setDelayedDoubleDeleteEnabled(false);
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        this.service.evict("product", "1001");

        assertThat(cache.containsLocal("1001")).isFalse();
        verify(this.scheduler, times(0)).schedule(any(Runnable.class), any(Date.class));
    }

    @Test
    @DisplayName("延迟任务的执行时间应晚于配置的延迟")
    void delayedEvictShouldRunAfterConfiguredDelay() {
        this.properties.setDelayedDoubleDeleteEnabled(true);
        this.properties.setDoubleDeleteDelay(java.time.Duration.ofSeconds(1));
        this.cacheManager.getCache("product");

        this.service.evict("product", "1001");

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(this.scheduler).schedule(any(Runnable.class), dateCaptor.capture());

        long scheduledIn = dateCaptor.getValue().getTime() - System.currentTimeMillis();
        assertThat(scheduledIn).isBetween(500L, 2_000L);
    }

    @Test
    @DisplayName("广播消息到达时应清理本节点 L1，但不动 L2")
    void broadcastMessageShouldClearLocalOnly() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        // 模拟另一节点发来的失效消息
        this.service.onInvalidateMessage(CacheInvalidateMessage.of("product", "1001", "node-2"));

        assertThat(cache.containsLocal("1001")).as("L1 应被清理").isFalse();
        assertThat(this.redis.contains(FakeRedis.redisKey("product", "1001")))
                .as("L2 由发起方删除，本节点不应重复删").isTrue();
    }

    @Test
    @DisplayName("PATTERN 类型的广播消息应清空对应缓存名的整个 L1")
    void patternMessageShouldClearWholeCache() {
        MultiLevelCache product = (MultiLevelCache) this.cacheManager.getCache("product");
        MultiLevelCache list = (MultiLevelCache) this.cacheManager.getCache("product:list");
        product.put("1001", "机械键盘");
        product.put("1002", "人体工学椅");
        list.put("键盘", "查询结果");

        this.service.onInvalidateMessage(CacheInvalidateMessage.ofAll("product", "node-2"));

        assertThat(product.localSize()).isZero();
        assertThat(list.localSize()).as("其它缓存不受影响").isEqualTo(1);
    }

    @Test
    @DisplayName("未知缓存名的广播消息应被安全忽略")
    void unknownCacheMessageShouldBeIgnored() {
        this.service.onInvalidateMessage(CacheInvalidateMessage.of("never-used", "1", "node-2"));

        assertThat(this.cacheManager.getCacheNames()).isEmpty();
    }

    @Test
    @DisplayName("空消息应被安全忽略")
    void nullMessageShouldBeIgnored() {
        this.service.onInvalidateMessage(null);
        this.service.onInvalidateMessage(new CacheInvalidateMessage());

        assertThat(this.cacheManager.getCacheNames()).isEmpty();
    }

    @Test
    @DisplayName("广播发布失败不应影响写操作主流程")
    void broadcastFailureShouldNotBreakWrite() {
        RedisTemplate<String, Object> broken = mock(RedisTemplate.class);
        // convertAndSend 返回 void，用 doThrow 触发异常
        org.mockito.Mockito.doThrow(new RuntimeException("Redis 不可用"))
                .when(broken).convertAndSend(anyString(), ArgumentMatchers.<Object>any());
        ReflectionTestUtils.setField(this.service, "redisTemplate", broken);

        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        // 广播失败时缓存仍然要被删除，不能抛异常
        this.service.evict("product", "1001");
        assertThat(cache.containsLocal("1001")).isFalse();
    }

    @Test
    @DisplayName("evictAll 应清空整个缓存名并广播")
    void evictAllShouldClearWholeCache() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "机械键盘");
        cache.put("1002", "人体工学椅");

        this.service.evictAll("product");

        assertThat(cache.localSize()).isZero();
        assertThat(this.redis.size()).isZero();
    }

    @Test
    @DisplayName("reconcile 应通过缓存管理器完成对账")
    void reconcileShouldDelegateToManager() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "机械键盘");
        this.redis.store().remove(FakeRedis.redisKey("product", "1001"));

        this.service.reconcile();

        assertThat(cache.localSize()).isZero();
    }

    @Test
    @DisplayName("调度器返回 null 时不应影响失效流程")
    void shouldTolerateNullScheduledFuture() {
        org.mockito.Mockito.doReturn(null).when(this.scheduler).schedule(any(Runnable.class), any(Date.class));
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        this.service.evict("product", "1001");

        assertThat(cache.containsLocal("1001")).isFalse();
    }

    @Test
    @DisplayName("事务中失效应推迟到提交之后，避免并发读回填未提交的数据")
    void evictShouldDeferUntilAfterCommitInTransaction() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        TransactionSynchronizationManager.initSynchronization();
        try {
            this.service.evict("product", "1001");

            assertThat(cache.containsLocal("1001"))
                    .as("事务提交前不应删缓存，否则并发读会回填未提交的新值").isTrue();

            // 模拟事务提交
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            assertThat(cache.containsLocal("1001")).as("提交后应完成失效").isFalse();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("非事务环境下失效应立即生效")
    void evictShouldRunImmediatelyWithoutTransaction() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        this.service.evict("product", "1001");

        assertThat(cache.containsLocal("1001")).isFalse();
    }

}