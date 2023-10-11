# springboot-kafka

Spring Boot 集成 Kafka

**Kafka**

一个由 Linkedin 公司开发的分布式、支持分区的（partition）、多副本的（replica）、基于 zookeeper 协调的消息系统，以其高吞吐量低延迟、持久性、容错性等特点，广泛应用于大数据领域。

**Spring Boot 整合 Kafka**

1）引入依赖

```
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

2）配置参数

```yaml
spring:
  kafka:
    # kafka服务地址
    bootstrap-servers: localhost:9092
    producer:
      # 发生错误后，消息重发的次数。
      retries: 0
      #当有多个消息需要被发送到同一个分区时，生产者会把它们放在同一个批次里。该参数指定了一个批次可以使用的内存大小，按照字节数计算。
      batch-size: 16384
      # 键的序列化方式
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      # 值的序列化方式
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      # acks=0 ： 生产者在成功写入消息之前不会等待任何来自服务器的响应。
      # acks=1 ： 只要集群的首领节点收到消息，生产者就会收到一个来自服务器成功响应。
      # acks=all ：只有当所有参与复制的节点全部收到消息时，生产者才会收到一个来自服务器的成功响应。
      acks: 1
    consumer:
      # 消费者组
      group-id: test-consumer-group
      # 自动提交的时间间隔 在spring boot 2.X 版本中这里采用的是值的类型为Duration 需要符合特定的格式，如1S,1M,2H,5D
      auto-commit-interval: 1S
      # 该属性指定了消费者在读取一个没有偏移量的分区或者偏移量无效的情况下该作何处理：
      # latest（默认值）在偏移量无效的情况下，消费者将从最新的记录开始读取数据（在消费者启动之后生成的记录）
      # earliest ：在偏移量无效的情况下，消费者将从起始位置读取分区的记录
      auto-offset-reset: earliest
      # 键的反序列化方式
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      # 值的反序列化方式
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

3）消费者

```java
@Slf4j
@Component
@KafkaListener(topics = { "msg" }, groupId = "test-consumer-group")
public class MsgConsumer {

    @KafkaHandler
    public void receive(String message) {
        log.info("消费者接收到的消息：" + message);
    }

}
```

4）生产者

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MsgProducer {

    private final KafkaTemplate kafkaTemplate;

    public void send(String message) {
        kafkaTemplate.send("msg", message);
        log.info("生产者发送成功");
    }

}
```

5）测试

```java
@SpringBootTest
public class ProducerTest {

    @Autowired
    private MsgProducer msgProducer;

    @Test
    public void sendTest() {
        msgProducer.send("绝密文件");
        while (true) {} // 虽然启动了SpringBootTest，但为了不直接退出需要阻塞测试线程
    }

}
########################################
2023-10-11 14:14:18.209  INFO 28584 --- [           main] com.slm.kafka.producer.MsgProducer       : 生产者发送成功
2023-10-11 14:14:18.233  INFO 28584 --- [ntainer#0-0-C-1] com.slm.kafka.consumer.MsgConsumer       : 消费者接收到的消息：绝密文件
```

