# springboot-rabbitmq

Spring Boot 集成 RabbitMQ

RabbitMQ是一款使用Erlang语言开发的，实现AMQP(高级消息队列协议)的开源消息中间件。首先要知道一些RabbitMQ的特点，官网可查：

- 可靠性。支持持久化，传输确认，发布确认等保证了MQ的可靠性。
- 灵活的分发消息策略。这应该是RabbitMQ的一大特点。在消息进入MQ前由Exchange(交换机)进行路由消息。分发消息策略有：简单模式、工作队列模式、发布订阅模式、路由模式、通配符模式。
- 支持集群。多台RabbitMQ服务器可以组成一个集群，形成一个逻辑Broker。
- 多种协议。RabbitMQ支持多种消息队列协议，比如 STOMP、MQTT 等等。
- 支持多种语言客户端。RabbitMQ几乎支持所有常用编程语言，包括 Java、.NET、Ruby 等等。
- 可视化管理界面。RabbitMQ提供了一个易用的用户界面，使得用户可以监控和管理消息 Broker。
- 插件机制。RabbitMQ提供了许多插件，可以通过插件进行扩展，也可以编写自己的插件。

Docker安装

> docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management

Spring Boot 整合 RabbitMQ

1）引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

2）添加配置

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

3）创建Direct交换机

```java
@Configuration
public class DirectRabbitConfig {

    @Bean
    public Queue rabbitmqDemoDirectQueue() {
        /**
         * 1、name:    队列名称
         * 2、durable: 是否持久化
         * 3、exclusive: 是否独享、排外的。如果设置为true，定义为排他队列。则只有创建者可以使用此队列。也就是private私有的。
         * 4、autoDelete: 是否自动删除。也就是临时队列。当最后一个消费者断开连接后，会自动删除。
         * */
        return new Queue("demo-queue", true, false, false);
    }

    @Bean
    public DirectExchange rabbitmqDemoDirectExchange() {
        /**
         * 1、name:    交换器名称
         * 2、durable: 是否持久化
         * 4、autoDelete: 是否自动删除
         * */
        return new DirectExchange("demo-exchange", true, false);
    }

    @Bean
    public Binding bindDirect() {
        return BindingBuilder.bind(rabbitmqDemoDirectQueue()).to(rabbitmqDemoDirectExchange()).with("routing");
    }

}
```

4）配置发送服务

```java
@Component
@RequiredArgsConstructor
public class Publisher {

    private final RabbitTemplate rabbitTemplate;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void sendMsg(String msg) {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String sendTime = sdf.format(new Date());
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("sendTime", sendTime);
        map.put("msg", msg);
        rabbitTemplate.convertAndSend("demo-exchange", "routing", map);
    }

}
```

5）测试发送消息，访问 rabbitmq 管理页面

地址：http://localhost:15672/

密码：guest / guest

此时 Exchanges 和 Queues 内会都出来刚创建的交换机和队列

6）创建消费者消费消息

```java
@Component
public class Consumer {

    @RabbitHandler
    @RabbitListener(queues = { "demo-queue" })
    public void process(Map map) {
        System.out.println("get msg: " + map.toString());
    }

}
```

此时启动程序就能看到消费者接收到消息并消费了！

```
get msg: {msg=123, requestId=c3d86f7c05054d1eb3df34efc4a9a09d, sendTime=2024-02-22 18:36:08}
```

via https://developer.aliyun.com/article/769883
