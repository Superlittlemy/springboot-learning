package com.slm.multilevelcache.consistency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * 失效广播订阅端
 *
 * <p>每个应用节点一个实例，订阅统一的失效频道，收到消息后清理本节点的 L1。</p>
 */
@Slf4j
public class CacheInvalidateListener implements MessageListener {

    private final CacheConsistencyService consistencyService;

    private final ObjectMapper objectMapper;

    public CacheInvalidateListener(CacheConsistencyService consistencyService, ObjectMapper objectMapper) {
        this.consistencyService = consistencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            CacheInvalidateMessage invalidateMessage = objectMapper.readValue(body, CacheInvalidateMessage.class);
            consistencyService.onInvalidateMessage(invalidateMessage);
        } catch (Exception e) {
            // 单条消息解析失败不能影响订阅线程存活，否则整个节点会停止接收失效广播
            log.warn("解析缓存失效广播失败：{}", e.getMessage());
        }
    }

}