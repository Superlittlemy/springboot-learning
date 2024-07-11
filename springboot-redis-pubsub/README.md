# springboot-redis-pubsub

Spring Boot Redis 发布订阅模式

使用Redis实现消息的发布订阅，由生产者（pub）发布消息，消费者（sub）消费消息。
这里有个 channel 的概念，就是一个通道，消费者订阅这个通道，然后发布者在这个通道发布消息，订阅这个通道的消费者都可以消费到。

Redis 命令

- subscribe topic：订阅主题
- psubscribe topic：pattern 匹配主题
- publish topic message：发布消息

Spring Boot 实现

1）引入依赖

```java
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2）创建监听类

```java
@Slf4j
@Component
public class RedisReceiver implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("listener 1 receive message: {}", message);
    }

}
```

3）配置监听（此处演示了多个监听）

```java
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
```

4）发布消息测试

```java
@SpringBootTest
public class SendMessageTest {

    @Value("${redis.topic.test}")
    private String testTopic;
    @Resource
    PublishService publishService;

    @Test
    void send() {
        publishService.sendMsg(testTopic, "test message");
        while (true) {}
    }

}
```

测试发送消息，控制台会受到两条监听的消息！