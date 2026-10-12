package com.slm.multilevelcache.consistency;

import com.slm.multilevelcache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis 的分布式锁
 *
 * <p>加锁用 {@code SET key token NX EX ttl}，一步完成「判重 + 写入 + 设过期」，
 * 避免 SETNX 与 EXPIRE 分两步时进程崩溃留下永不过期的锁。</p>
 *
 * <p>解锁必须校验 token 并用 Lua 保证原子性，否则会出现：
 * A 的锁超时自动释放 → B 抢到锁 → A 恢复后执行 DEL，把 B 的锁误删。</p>
 */
@Slf4j
public class RedisDistributedLock implements DistributedLock {

    /**
     * 仅当锁的持有者与 token 相同时才删除
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    private final CacheProperties properties;

    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate, CacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean tryLock(String lockKey, String token) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(fullKey(lockKey), token, properties.getRebuildLockTtl());
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            // Redis 不可用时选择「抢不到锁」，让调用方走降级路径而不是把数据库打穿
            log.warn("获取重建锁失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public void unlock(String lockKey, String token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT,
                    Collections.singletonList(fullKey(lockKey)), token);
        } catch (Exception e) {
            // 解锁失败不抛异常：锁自带 TTL，最终一定会自动释放
            log.warn("释放重建锁失败，等待 TTL 自动过期：{}", e.getMessage());
        }
    }

    /**
     * 生成一个新的持有者标识
     */
    public String newToken() {
        return UUID.randomUUID().toString();
    }

    private String fullKey(String lockKey) {
        return properties.getLockKeyPrefix() + lockKey;
    }

}