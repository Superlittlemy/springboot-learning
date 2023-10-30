package com.slm.rediscache.utils;

import com.slm.rediscache.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<Object, Object> redisTemplate;

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("普通缓存放入失败", e);
            throw new BizException("普通缓存放入失败");
        }
    }

    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
        } catch (Exception e) {
            log.error("普通缓存放入失败", e);
            throw new BizException("普通缓存放入失败");
        }
    }

    public void set(String key, Object value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, value, duration);
        } catch (Exception e) {
            log.error("普通缓存放入失败", e);
            throw new BizException("普通缓存放入失败");
        }
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long incr(String key, long delta) {
        if (delta < 0) {
            throw new BizException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

}
