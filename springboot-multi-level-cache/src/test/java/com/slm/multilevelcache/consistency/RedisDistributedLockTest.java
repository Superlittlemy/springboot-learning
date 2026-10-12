package com.slm.multilevelcache.consistency;

import com.slm.multilevelcache.config.CacheProperties;
import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分布式重建锁测试
 */
class RedisDistributedLockTest {

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOps;

    private CacheProperties properties;

    private RedisDistributedLock lock;

    @BeforeEach
    void setUp() {
        this.redisTemplate = mock(RedisTemplate.class);
        // opsForValue() 必须先在 when() 之外取出，
        // 否则 Mockito 会把对它的调用当成待验证的桩，导致 InvalidUseOfMatchers
        this.valueOps = mock(ValueOperations.class);
        when(this.redisTemplate.opsForValue()).thenReturn(this.valueOps);
        this.properties = FakeRedis.properties();
        this.properties.setLockKeyPrefix("multi-level-cache:lock:");
        this.lock = new RedisDistributedLock(this.redisTemplate, this.properties);
    }

    @Test
    @DisplayName("加锁应使用带过期时间的 SETNX，避免崩溃后留下永不过期的锁")
    void tryLockShouldUseSetIfAbsentWithTtl() {
        when(this.valueOps.setIfAbsent(anyString(), any(Object.class), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        boolean acquired = this.lock.tryLock("product:1001", "token-1");

        assertThat(acquired).isTrue();
        verify(this.valueOps).setIfAbsent(
                eq("multi-level-cache:lock:product:1001"), eq("token-1"), any(Duration.class));
    }

    @Test
    @DisplayName("锁已被占用时加锁应失败")
    void shouldFailWhenLockHeld() {
        when(this.valueOps.setIfAbsent(anyString(), any(Object.class), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        assertThat(this.lock.tryLock("product:1001", "token-2")).isFalse();
    }

    @Test
    @DisplayName("Redis 故障时应视为抢锁失败，让调用方走降级而不是放大并发")
    void shouldFailWhenRedisUnavailable() {
        when(this.valueOps.setIfAbsent(anyString(), any(Object.class), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis 不可用"));

        assertThat(this.lock.tryLock("product:1001", "token-1"))
                .as("Redis 挂掉时不能认为加锁成功，否则所有请求都会回源").isFalse();
    }

    @Test
    @DisplayName("解锁应通过 Lua 脚本原子校验持有者")
    void unlockShouldUseLuaScript() {
        this.lock.unlock("product:1001", "token-1");

        verify(this.redisTemplate).execute(any(), anyList(), eq("token-1"));
    }

    @Test
    @DisplayName("解锁失败不应抛异常，锁会靠 TTL 自动过期")
    void unlockShouldNotThrowOnRedisError() {
        when(this.redisTemplate.execute(any(), anyList(), any()))
                .thenThrow(new RuntimeException("Redis 不可用"));

        this.lock.unlock("product:1001", "token-1");
        // 未抛异常即通过
    }

    @Test
    @DisplayName("每次加锁应生成不同的持有者标识")
    void tokensShouldBeUnique() {
        assertThat(this.lock.newToken()).isNotEqualTo(this.lock.newToken());
    }

    @Test
    @DisplayName("与内存 Redis 替身联调：同一把锁只能被抢到一次")
    void shouldOnlyAcquireOnceWithSharedLock() {
        FakeRedis fake = new FakeRedis();
        RedisDistributedLock realLock = new RedisDistributedLock(fake.template(), this.properties);

        assertThat(realLock.tryLock("product:1001", "token-a")).isTrue();
        assertThat(realLock.tryLock("product:1001", "token-b"))
                .as("锁未被释放前其它线程应抢不到").isFalse();
    }

}