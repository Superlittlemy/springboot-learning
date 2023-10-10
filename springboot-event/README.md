# springboot-event

Spring Boot Event 事件发布与监听

> Spring Event 基于观察者模式，允许你定义一种订阅机制， 可在对象事件发生时通知观察对象做出相应动作。

---

## Spring Event 同步使用

此案例使用 ApplicationListener 接口实现监听

- 定义事件

  ```java
  /**
   * 价格检查事件
   */
  public class PriceCheckEvent extends ApplicationEvent {
  
      private BigDecimal price;
  
      public PriceCheckEvent(Object source, BigDecimal price) {
          super(source);
          this.price = price;
      }
  
      public BigDecimal getPrice() {
          return price;
      }
  
      public void setPrice(BigDecimal price) {
          this.price = price;
      }
  
  }
  ```

- 监听事件

  ```java
  @Slf4j
  @Component
  public class PriceCheckListener implements ApplicationListener<PriceCheckEvent> {
  
      @Override
      public void onApplicationEvent(PriceCheckEvent priceCheckEvent) {
          try {
              Thread.sleep(3000);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
          log.info("价格检验通过");
      }
  
  }
  ```

- 发布事件

  ```java
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class OrderService {
  
      private final ApplicationContext applicationContext;
  
      public void buy(Order order) {
          long start = System.currentTimeMillis();
          // 1. 检查价格
          applicationContext.publishEvent(new PriceCheckEvent(this, order.getPrice()));
          // 2. 创建订单
          // 3. 发送邮件
          
          long end = System.currentTimeMillis();
          log.info("下单完成，总耗时：" + (end - start));
      }
  
  }
  ```

- 测试结果

  > 2023-10-10 10:56:48.786  INFO 3192 --- [nio-8080-exec-1] c.slm.event.controller.OrderController   : 进入下单流程
  > 2023-10-10 10:56:51.800  INFO 3192 --- [nio-8080-exec-1] c.slm.event.listener.PriceCheckListener  : 价格检验通过
  > 2023-10-10 10:56:51.803  INFO 3192 --- [nio-8080-exec-1] com.slm.event.service.OrderService       : 下单完成，总耗时：3015

## Spring Event 异步使用

此案例使用 EventListener 注解实现监听

- 定义事件

  ```java
  /**
   * 邮件事件
   */
  @Data
  @AllArgsConstructor
  public class EmailEvent {
  
      private String content;
  
  }
  ```

- 开启异步

  ```java
  @EnableAsync // 添加该注解开启异步功能
  @SpringBootApplication
  public class Application {
  
      public static void main(String[] args) {
          SpringApplication.run(Application.class, args);
      }
  
  }
  ```

- 监听事件

  ```java
  @Slf4j
  @Component
  public class EmailListener {
  
      @Async // 添加该注解标记该方法异步执行
      @EventListener(EmailEvent.class)
      public void listen(EmailEvent event) throws InterruptedException {
          Thread.sleep(3000);
          log.info("发送邮件，内容为：" + event.getContent());
      }
  
  }
  ```

- 发布事件

  ```java
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class OrderService {
  
      private final ApplicationContext applicationContext;
  
      public void buy(Order order) {
          long start = System.currentTimeMillis();
          // 1. 检查价格
          applicationContext.publishEvent(new PriceCheckEvent(this, order.getPrice()));
          // 2. 创建订单
          // 3. 发送邮件
          applicationContext.publishEvent(new EmailEvent("采购了新鲜蔬菜100公斤"));
          long end = System.currentTimeMillis();
          log.info("下单完成，总耗时：" + (end - start));
      }
  
  }
  ```

- 测试结果

  > 2023-10-10 10:56:48.786  INFO 3192 --- [nio-8080-exec-1] c.slm.event.controller.OrderController   : 进入下单流程
  > 2023-10-10 10:56:51.800  INFO 3192 --- [nio-8080-exec-1] c.slm.event.listener.PriceCheckListener  : 价格检验通过
  > 2023-10-10 10:56:51.803  INFO 3192 --- [nio-8080-exec-1] com.slm.event.service.OrderService       : 下单完成，总耗时：3015
  > 2023-10-10 10:56:54.815  INFO 3192 --- [         task-1] com.slm.event.listener.EmailListener     : 发送邮件，内容为：采购了新鲜蔬菜100公斤

  PS：邮件在下单完成后3秒发送完毕，体现了异步解耦