package com.slm.multilevelcache.support;

import com.slm.multilevelcache.config.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试替身：一个可用内存 Map 支撑的 RedisTemplate。
 *
 * <p>单元测试不应该依赖真实 Redis，否则既慢又会在 CI 上不稳定。
 * 这里用 Mockito 把常用操作接到一个 Map 上，既能验证跨层读写行为，
 * 又能在测试中直接检查 L2 里到底存了什么。</p>
 */
public class FakeRedis {

    /**
     * L2 的实际内容，key 使用带前缀的完整 redis key
     */
    private final Map<String, Object> store = new LinkedHashMap<>();

    /**
     * 记录每个 key 被写入时携带的 TTL，用于断言空值用了更短的 TTL
     */
    private final Map<String, Duration> ttls = new LinkedHashMap<>();

    private final RedisTemplate<String, Object> template;

    public FakeRedis() {
        this.template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);

        when(this.template.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString()))
                .thenAnswer(inv -> this.store.get(inv.getArgument(0, String.class)));
        // ValueOperations.set 返回 void，必须用 doAnswer 而非 when(...).thenAnswer(...)
        doAnswer(inv -> {
            this.store.put(inv.getArgument(0, String.class), inv.getArgument(1));
            this.ttls.put(inv.getArgument(0, String.class), inv.getArgument(2, Duration.class));
            return null;
        }).when(valueOps).set(anyString(), any(Object.class), any(Duration.class));
        when(valueOps.setIfAbsent(anyString(), any(Object.class), any(Duration.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0, String.class);
            if (this.store.containsKey(key)) {
                return Boolean.FALSE;
            }
            this.store.put(key, inv.getArgument(1));
            this.ttls.put(key, inv.getArgument(2, Duration.class));
            return Boolean.TRUE;
        });
        when(this.template.delete(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0, String.class);
            this.ttls.remove(key);
            return this.store.remove(key) != null;
        });
        // clear() 走的是 delete(Collection) 重载，返回 Long，必须单独桩住
        when(this.template.delete(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<String> keys = inv.getArgument(0);
            long removed = 0;
            for (String key : keys) {
                this.ttls.remove(key);
                if (this.store.remove(key) != null) {
                    removed++;
                }
            }
            return removed;
        });
        when(this.template.hasKey(anyString()))
                .thenAnswer(inv -> this.store.containsKey(inv.getArgument(0, String.class)));
        when(this.template.keys(anyString())).thenAnswer(inv -> {
            String pattern = inv.getArgument(0, String.class);
            String regex = pattern.replace("*", ".*");
            // RedisTemplate.keys 的返回类型是 Set，这里必须返回 Set，否则调用方会 CCE
            return this.store.keySet().stream()
                    .filter(k -> k.matches(regex))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        });
    }

    public RedisTemplate<String, Object> template() {
        return this.template;
    }

    public Object rawGet(String fullKey) {
        return this.store.get(fullKey);
    }

    public void rawPut(String fullKey, Object value) {
        this.store.put(fullKey, value);
    }

    public boolean contains(String fullKey) {
        return this.store.containsKey(fullKey);
    }

    public Duration ttlOf(String fullKey) {
        return this.ttls.get(fullKey);
    }

    public int size() {
        return this.store.size();
    }

    public Map<String, Object> store() {
        return this.store;
    }

    /**
     * 让下一次 L2 访问抛出异常，用于验证缓存故障时的降级行为
     */
    public void failOnGet() {
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis 不可用"));
        doThrow(new RuntimeException("Redis 不可用"))
                .when(valueOps).set(anyString(), any(Object.class), any(Duration.class));
        when(this.template.opsForValue()).thenReturn(valueOps);
    }

    /**
     * 构造一份默认配置
     */
    public static CacheProperties properties() {
        CacheProperties properties = new CacheProperties();
        properties.setCacheNullValue(true);
        return properties;
    }

    /**
     * 拼出某个缓存名 + key 对应的完整 redis key，与 MultiLevelCache 内部规则一致
     */
    public static String redisKey(String cacheName, Object key) {
        return "multi-level-cache:" + cacheName + ":" + key;
    }

}