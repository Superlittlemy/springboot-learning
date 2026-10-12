package com.slm.multilevelcache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slm.multilevelcache.cache.MultiLevelCacheManager;
import com.slm.multilevelcache.consistency.CacheConsistencyService;
import com.slm.multilevelcache.consistency.CacheInvalidateListener;
import com.slm.multilevelcache.consistency.DistributedLock;
import com.slm.multilevelcache.consistency.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 多级缓存装配
 */
@Slf4j
@Configuration
public class CacheConfiguration {

    /**
     * 业务对象序列化器。
     *
     * <p>开启类型信息，保证多态的返回值反序列化回原类型；L1 里存的是同一个对象实例，
     * 不经过序列化，只有跨节点的 L2 才需要它。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jsonSerializer.setObjectMapper(buildObjectMapper());
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // key 用字符串，可读且方便在 redis-cli 里排查
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 广播消息的负载序列化器：JSON 即可，避免带上类名元数据
     */
    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * 业务对象序列化用的 ObjectMapper
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public MultiLevelCacheManager cacheManager(RedisTemplate<String, Object> redisTemplate,
                                               CacheProperties properties) {
        return new MultiLevelCacheManager(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLock distributedLock(RedisTemplate<String, Object> redisTemplate,
                                           CacheProperties properties) {
        return new RedisDistributedLock(redisTemplate, properties);
    }

    /**
     * 失效广播订阅容器
     *
     * <p>每个应用节点都会收到频道消息，包括节点自己发出的那条（Redis Pub/Sub 的回环特性），
     * 这一点在 {@link CacheConsistencyService#onInvalidateMessage} 中已被考虑。</p>
     *
     * <p>单节点部署不需要广播，用 {@code multi-level-cache.broadcast-enabled=false}
     * 关闭后可以省掉一个长连接。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "multi-level-cache", name = "broadcast-enabled",
            havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheConsistencyService consistencyService,
            ObjectMapper objectMapper,
            CacheProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new CacheInvalidateListener(consistencyService, objectMapper),
                new ChannelTopic(properties.getInvalidateChannel()));
        log.info("订阅缓存失效广播频道：{}", properties.getInvalidateChannel());
        return container;
    }

    /**
     * 延迟双删等延时任务的调度线程池
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolTaskScheduler cacheTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("cache-scheduler-");
        // 优雅停机：等待正在执行的延迟双删任务结束，避免关机时漏掉二次删除
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

}