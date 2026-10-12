package com.slm.multilevelcache.consistency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slm.multilevelcache.cache.MultiLevelCache;
import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 失效广播消息的序列化与订阅测试
 *
 * <p>消息必须能跨节点正确还原，否则广播到了对端也解析不出来。</p>
 */
class CacheInvalidateListenerTest {

    private ObjectMapper objectMapper;

    private MultiLevelCacheManager cacheManager;

    private CacheConsistencyService service;

    private CacheInvalidateListener listener;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.cacheManager = new MultiLevelCacheManager(new FakeRedis().template(), FakeRedis.properties());

        this.service = new CacheConsistencyService();
        ReflectionTestUtils.setField(this.service, "cacheManager", this.cacheManager);
        ReflectionTestUtils.setField(this.service, "properties", FakeRedis.properties());
        ReflectionTestUtils.setField(this.service, "nodeId", "test-node");

        this.listener = new CacheInvalidateListener(this.service, this.objectMapper);
    }

    @Test
    @DisplayName("SINGLE 类型消息可完整序列化并还原")
    void singleMessageShouldRoundTrip() throws Exception {
        CacheInvalidateMessage original = CacheInvalidateMessage.of("product", "1001", "node-2");

        String json = this.objectMapper.writeValueAsString(original);
        CacheInvalidateMessage restored = this.objectMapper.readValue(json, CacheInvalidateMessage.class);

        assertThat(restored.getCacheName()).isEqualTo("product");
        assertThat(restored.getKey()).isEqualTo("1001");
        assertThat(restored.getType()).isEqualTo(CacheInvalidateType.SINGLE);
        assertThat(restored.getOriginNode()).isEqualTo("node-2");
    }

    @Test
    @DisplayName("PATTERN 类型消息可完整序列化并还原")
    void patternMessageShouldRoundTrip() throws Exception {
        CacheInvalidateMessage original = CacheInvalidateMessage.ofAll("product:list", "node-2");

        String json = this.objectMapper.writeValueAsString(original);
        CacheInvalidateMessage restored = this.objectMapper.readValue(json, CacheInvalidateMessage.class);

        assertThat(restored.getType()).isEqualTo(CacheInvalidateType.PATTERN);
        assertThat(restored.getKey()).isNull();
    }

    @Test
    @DisplayName("监听器收到合法消息时应清理本地缓存")
    void listenerShouldEvictLocalOnMessage() throws Exception {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "旧值");

        String json = this.objectMapper.writeValueAsString(
                CacheInvalidateMessage.of("product", "1001", "node-2"));
        this.listener.onMessage(messageOf(json), null);

        assertThat(cache.containsLocal("1001")).isFalse();
    }

    @Test
    @DisplayName("解析失败不应让订阅线程崩溃，否则该节点会停止接收所有广播")
    void malformedMessageShouldNotKillListener() {
        MultiLevelCache cache = (MultiLevelCache) this.cacheManager.getCache("product");
        cache.put("1001", "值");

        this.listener.onMessage(messageOf("这不是合法 JSON"), null);

        // 未抛异常即通过，且不影响后续正常消息的处理
        assertThat(cache.containsLocal("1001")).isTrue();
    }

    /**
     * 构造一条 Redis 广播消息。注意 DefaultMessage 的构造参数顺序是 (channel, body)。
     */
    private static DefaultMessage messageOf(String body) {
        return new DefaultMessage(new byte[0], body.getBytes(StandardCharsets.UTF_8));
    }

}