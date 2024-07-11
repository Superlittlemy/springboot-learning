package com.slm.redis.pubsub.config;

import com.slm.redis.pubsub.listener.RedisReceiver;
import com.slm.redis.pubsub.listener.RedisReceiver2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    @Value("${redis.topic.test}")
    private String testTopic;

    @Bean
    public RedisMessageListenerContainer listenerContainer(RedisConnectionFactory factory,
                                                           @Qualifier("listener1") MessageListenerAdapter messageListenerAdapter,
                                                           @Qualifier("listener2") MessageListenerAdapter messageListenerAdapter2) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic(testTopic));
        container.addMessageListener(messageListenerAdapter2, new PatternTopic(testTopic));
        return container;
    }

    @Bean("listener1")
    public MessageListenerAdapter messageListenerAdapter(RedisReceiver redisReceiver){
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisReceiver,
                "onMessage");
        return messageListenerAdapter;
    }

    @Bean("listener2")
    public MessageListenerAdapter messageListenerAdapter(RedisReceiver2 redisReceiver){
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisReceiver,
                "onMessage");
        return messageListenerAdapter;
    }

}
