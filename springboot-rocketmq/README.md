# springboot-rocketmq

> Spring Boot 集成 RocketMQ 消息队列

## Docker Compose 安装

```yaml
version: '3.8'
services:
  namesrv:
    image: apache/rocketmq:5.3.2
    container_name: rmqnamesrv
    ports:
      - 9876:9876
    networks:
      - rocketmq
    command: sh mqnamesrv
  broker:
    image: apache/rocketmq:5.3.2
    container_name: rmqbroker
    ports:
      - 10909:10909
      - 10911:10911
      - 10912:10912
    environment:
      - NAMESRV_ADDR=rmqnamesrv:9876
    volumes:
      - ./broker.conf:/home/rocketmq/rocketmq-5.3.2/conf/broker.conf
    depends_on:
      - namesrv
    networks:
      - rocketmq
    command: sh mqbroker -c /home/rocketmq/rocketmq-5.3.2/conf/broker.conf
  proxy:
    image: apache/rocketmq:5.3.2
    container_name: rmqproxy
    networks:
      - rocketmq
    depends_on:
      - broker
      - namesrv
    ports:
      - 8080:8080
      - 8081:8081
    restart: on-failure
    environment:
      - NAMESRV_ADDR=rmqnamesrv:9876
    command: sh mqproxy
  # 新增 Dashboard 服务
  dashboard:
    image: apacherocketmq/rocketmq-dashboard:latest
    container_name: rmqdashboard
    ports:
      - 8181:8082  # 宿主机 8181 映射到容器的 8080，避免与 proxy 的 8080 冲突
    depends_on:
      - namesrv
    networks:
      - rocketmq
    environment:
      # 关键：指向 namesrv 容器的服务名和端口，而非 localhost
      JAVA_OPTS: "-Drocketmq.namesrv.addr=rmqnamesrv:9876 -Drocketmq.config.isVIPChannel=false"
networks:
  rocketmq:
    driver: bridge
```

注意：Docker 启动 Broker 必须在 broker.conf 内添加 `brokerIP1`，否则注册为容器内 IP，程序发送消息会报错。挂载外部 broker.conf，启动命令指定配置文件 `sh mqbroker -c /home/rocketmq/rocketmq-5.3.2/conf/broker.conf`。Spirng Boot 2.7.18 依赖仅支持到 4.x 消息版本可以关闭 proxy 服务。

## Spring Boot 集成

1）添加依赖（基于 Spring Boot 2.7.18）

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.3</version>
</dependency>
```

2）配置 broker 连接和默认生产者

```yaml
rocketmq:
  name-server: 127.0.0.1:9876  # NameServer 地址
  producer:
    group: default-producer-group   # 生产者组名，必须唯一
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

### 生产者

#### 普通消息

```java
/**
 * 同步发送普通消息
 * @param msg 消息内容
 */
public void syncSendNormalMessage(String msg) {
    SendResult sendResult = rocketMQTemplate.syncSend("normal-message", msg);
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
        log.info("sync send normal message success: {}", sendResult);
    } else {
        log.error("sync send normal message failed: {}", sendResult);
    }
}

/**
 * 异步发送普通消息
 * @param msg 消息内容
 */
public void asyncSendNormalMessage(String msg) {
    rocketMQTemplate.asyncSend("normal-message", msg, new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            log.info("async send normal message success: {}", sendResult);
        }

        @Override
        public void onException(Throwable e) {
            log.error("async send normal message failed: {}", e.getMessage());
        }
    });
}
```

#### 延迟消息

```java
/**
 * 发送延迟消息
 * @param msg 消息内容
 */
public void sendDelayMessage(String msg, int delaySeconds) {
    Message<String> message = MessageBuilder.withPayload(msg).build();
    SendResult sendResult = rocketMQTemplate.syncSendDelayTimeSeconds("delay-message", message, delaySeconds);
    if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
        log.info("send delay message success: {}", sendResult);
    } else {
        log.error("send delay message failed: {}", sendResult);
    }
}
```

#### 顺序消息

```java
/**
 * 发送顺序消息
 * @param orderMessages 有序消息
 * @param queue 队列
 */
public void sendOrderlyMessage(String[] orderMessages, String queue) {
    Arrays.stream(orderMessages).forEach(msg -> {
        SendResult sendResult = rocketMQTemplate.syncSendOrderly("orderly-message", msg, queue);
        if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
            log.info("send orderly message success: {}", sendResult);
        } else {
            log.error("send orderly message failed: {}", sendResult);
        }
    });
}
```

#### 事务消息

##### 核心流程

1. **发送半事务消息**：生产者将消息发送至 Broker。Broker 将其持久化，但标记为“暂不能投递”（对消费者不可见），随后向生产者返回成功确认。
2. **执行本地事务**：生产者收到确认后，开始执行本地业务逻辑（如数据库操作）。
3. **提交二次确认**：生产者根据本地事务结果，向 Broker 发送 `Commit` 或 `Rollback` 指令。
   - **Commit**：Broker 将半消息标记为可投递，消费者此时才能消费。
   - **Rollback**：Broker 丢弃该消息，消费者不会收到。
4. **回查机制（兜底）**：若因网络问题或生产者重启，Broker 未收到二次确认（或收到 `Unknown` 状态），Broker 会定时反查生产者的本地事务最终状态，并根据结果决定提交或回滚。

##### 代码实现

独立事务 Template

```java
@ExtRocketMQTemplateConfiguration(group = "tx-producer-group")
public class TxRocketMQTemplate extends RocketMQTemplate {
}
```
发送消息

```java
/**
 * 发送事务消息
 * @param msg 消息内容
 */
public void sendTransactionMessage(String msg) {
    Message<String> message = MessageBuilder.withPayload(msg).build();
    TransactionSendResult transactionSendResult = txRocketMQTemplate.sendMessageInTransaction("transaction-message", message, null);
    if (SendStatus.SEND_OK == transactionSendResult.getSendStatus()) {
        log.info("send transaction message success: {}", transactionSendResult);
    } else {
        log.error("send transaction message failed: {}", transactionSendResult);
    }
}
```

本地事务及回查逻辑

```java

@Slf4j
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "txRocketMQTemplate")
public class OrderTxListener implements RocketMQLocalTransactionListener {

    /**
     * 在半消息发送成功后被回调
     *
     * @param msg 事务消息
     * @param arg 本地事务执行器的临时入参，仅在首次 executeLocalTransaction 调用时存在
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        log.info("executeLocalTransaction msg: {}, arg: {}", msg, arg);
        String orderId = (String) arg;
        try {
            // 执行本地数据库事务，例如创建订单
            // orderService.createOrder(orderId);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 只在二次确认丢失（网络问题、生产者重启等）时被 Broker 触发，用于“兜底”查询真实结果。
     *
     * @param msg 事务消息
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        log.info("checkLocalTransaction msg: {}", msg);
        String orderId = (String) msg.getHeaders().get("orderId");
        // 查询数据库确认本地事务是否成功
        // boolean exists = orderService.isOrderExist(orderId);
        // return exists ? COMMIT : ROLLBACK;
        return RocketMQLocalTransactionState.COMMIT;
    }
}
```

#### TAG消息

```java
/**
 * 发送带TAG消息
 * @param msg 消息内容
 * @param tag TAG名称
 */
public void sendTagFilterMessage(String msg, String tag) {
    rocketMQTemplate.convertAndSend("tag-message:" + tag, msg);
}
```

#### SQL92消息

```java
/**
 * 发送SQL92消息
 * @param msg 消息内容
 */
public void sendSQL92FilterMessage(String msg, Map<String, Object> headers) {
    MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(msg);
    headers.forEach(messageBuilder::setHeader);
    Message<String> message = messageBuilder.build();
    rocketMQTemplate.syncSend("sql92-message", message);
}
```

### 消费者

#### 普通消费

```java
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "normal-message",
        consumerGroup = "normal-group"
)
public class NormalMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("收到普通消息：{}", message);
    }

}
```

并行消费：通过修改参数 consumeThreadMin、consumeThreadMax 提升并行线程从而提高消费吞吐量

#### 顺序消费

顺序消息报错 [**官方建议**](https://rocketmq.apache.org/zh/docs/4.x/bestPractice/01bestpractice/#%E5%85%B3%E4%BA%8E%E6%9C%89%E5%BA%8F%E6%B6%88%E6%81%AF) 返回 `ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT` 作为替代。

```java
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "orderly-message",
        consumerGroup = "orderly-group", 
        consumeMode = ConsumeMode.ORDERLY
)
public class QueueMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("收到有序消息：{}", message);
    }

}
```

#### TAG条件消费

TAG 语法：

- 订阅单个：tag
- 订阅多个：tagA || tagB
- 订阅全部：*

```java
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "tag-message",
        consumerGroup = "tag-group",
        messageModel = MessageModel.BROADCASTING,
        selectorType = SelectorType.TAG,
        selectorExpression = "tagA || tagB"
)
public class TagAMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("收到 tagA or tagB 消息：{}", message);
    }

}
```

#### SQL92条件消费

> 必须在 Broker 的配置文件中显式开启 `enablePropertyFilter=true`

核心语法规则

1. 支持的运算符

- **数值比较**：`>`、`>=`、`<`、`<=`、`BETWEEN`、`=` 
- **字符比较**：`=`、`<>`（不等于）、`IN` 
- **空值判断**：`IS NULL`、`IS NOT NULL` 
- **逻辑运算**：`AND`、`OR`、`NOT` 

2. 支持的数据类型

- **数值**：整数或浮点数，如 `123`、`3.1415` 。
- **字符**：必须使用**单引号**，如 `'abc'` 。
- **布尔值**：`TRUE` 或 `FALSE` 。
- **NULL**：特殊常量。

3. 表达式示例
合法表达式如：`a > 5 AND b = 'abc'` 或 `(a > 10 AND a < 100) OR (b IS NOT NULL AND b=TRUE)` 。

```java
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "sql92-message",
        consumerGroup = "sql92-group",
        selectorType = SelectorType.SQL92,
        selectorExpression = "type = 'SQL92'"
)
public class SQL92MessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("收到 SL92 消息：{}", message);
    }

}
```

#### 广播模式

默认集群模式，同一消费者组只有一个消费者可以消费，广播模式下所有消费者都可以收到消息。

```java
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "broadcasting-message",
        consumerGroup = "broadcasting-group",
        messageModel = MessageModel.BROADCASTING
)
public class BroadcastingMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("广播接收者 收到广播消息：{}", message);
    }

}
```
