package com.slm.redislock.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 尝试获取锁
     *
     * @param key
     * @param value
     * @param expireTime 过期时间
     * @return
     */
    public Boolean tryLock(String key, Object value, long expireTime) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("尝试锁对象失败", e);
            return false;
        }
    }

    /**
     * 解锁
     * 需要根据value判断当前获得锁的是否为本线程生成的requestId，否则不解除别人的锁
     *
     * @param key
     * @param value
     */
    public Long unlock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        return this.redisTemplate.execute(redisScript, Collections.singletonList(key), value);
    }

}
