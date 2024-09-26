# springboot-task

Spring Boot Task 定时任务

在日常程序运行中，存在一些操作需要周期性定时运行的，例如：过期数据的清理、报表的预计算等。Spring Task 提供了简单易用无需第三方依赖的定时任务功能，另外还有 Quartz、XXL-Job 等第三框架。

**Spring Task**

> Spring 自带，在spring-context 模块下

1）在启动类添加 `@EnableScheduling` 注解开启定时任务

2）在需要周期性操作的类上加上 `@Scheduled` 即可，他有三个属性，代表不同的执行方式

- cron 表达式

- fixedDelay

  从上一次任务结束开始计算，间隔设定的时间执行

  ![](assets\7f1nu1pof4.png)

- fixedRate

  固定周期运行，由于 Spring Boot 定时任务默认是以单线程方式执行，所以如果前一个任务会阻塞后续任务执行。在该模式下，如果前一个任务耗时较长，在 fixedRate 任务周期到达时未完成造成阻塞，那任务就会超时执行，但不会丢失执行动作，如果阻塞较长多个任务堆积，可能阻塞释放后该模式下的任务密集执行。

  ![](assets\sy1i5at8f9.png)

**上代码**

```java
@Slf4j
@Component
public class PaymentSyncTask {

    private int i = 0;

    // 支付结果定时拉取
    @Scheduled(fixedDelay = 5000)
    public void sync() {
        log.info("拉取银行支付状态信息……");
    }

    // 报表定时刷新
    @Scheduled(fixedRate = 3000)
    public void dashboardRefresh() throws InterruptedException {
        i++;
        if (i % 3 == 0) {
            Thread.sleep(10000); // 模拟阻塞
        }
        log.info("固定每3S更新报表 " + i);
    }

    // 每日报表计算
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyReport() {
        log.info("每天凌晨统计日报");
    }

}
```

在第二个任务中，每三次模拟了一次超长耗时阻塞，让我们来看看执行效果：

```
2023-10-12 18:18:32.757  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 1
2023-10-12 18:18:32.758  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-12 18:18:35.768  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 2
2023-10-12 18:18:37.765  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-12 18:18:48.774  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 3
2023-10-12 18:18:48.775  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 4
2023-10-12 18:18:48.775  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-12 18:18:48.775  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 5
2023-10-12 18:18:58.776  INFO 23056 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 6
```

首先，让程序启动后，前两个定时任务开始了定时循环任务，从**更新报表**的第三次任务开始，阻塞效果产生，这次任务根据上一次任务的时间 `2023-10-12 18:18:35.768` 计算应该为 38 分执行，而由于模拟阻塞了 10S，导致后续操作都阻塞产生了任务堆积密集执行的情况。单线程的任务也让任务顺序不固定，这里的拉取银行支付状态掺杂在第 3、4、5 次更新报表任务里。

简单易用是 Spring Task 的最大优点，但单线程的限制在一定程度上可能会产生一些问题！

**TaskScheduler**

任务调度器

如果没有任务调度器，则生成一个单线程池执行定时任务，在源码中执行任务处也得到了引证

```java
protected void scheduleTasks() {
    if (this.taskScheduler == null) {
        this.localExecutor = Executors.newSingleThreadScheduledExecutor();
        this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
    }
    ……
}
```

以下是 TaskScheduler 的自动配置类

```java
@ConditionalOnClass({ThreadPoolTaskScheduler.class})
@Configuration(
    proxyBeanMethods = false
)
@EnableConfigurationProperties({TaskSchedulingProperties.class})
@AutoConfigureAfter({TaskExecutionAutoConfiguration.class})
public class TaskSchedulingAutoConfiguration {
    public TaskSchedulingAutoConfiguration() {
    }

    @Bean
    @ConditionalOnBean(
        name = {"org.springframework.context.annotation.internalScheduledAnnotationProcessor"}
    )
    @ConditionalOnMissingBean({SchedulingConfigurer.class, TaskScheduler.class, ScheduledExecutorService.class})
    public ThreadPoolTaskScheduler taskScheduler(TaskSchedulerBuilder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskSchedulerBuilder taskSchedulerBuilder(TaskSchedulingProperties properties, ObjectProvider<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
        TaskSchedulerBuilder builder = new TaskSchedulerBuilder();
        builder = builder.poolSize(properties.getPool().getSize());
        TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
        builder = builder.awaitTermination(shutdown.isAwaitTermination());
        builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
        builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
        builder = builder.customizers(taskSchedulerCustomizers);
        return builder;
    }
}
```

我们来配置以下线程池大小

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 10
```

配置后的运行情况

```
2023-10-13 11:08:23.027  INFO 17064 --- [   scheduling-3] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-13 11:08:23.028  INFO 17064 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 1
2023-10-13 11:08:26.031  INFO 17064 --- [   scheduling-2] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 2
2023-10-13 11:08:28.030  INFO 17064 --- [   scheduling-3] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-13 11:08:33.045  INFO 17064 --- [   scheduling-4] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-13 11:08:38.049  INFO 17064 --- [   scheduling-2] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-13 11:08:39.044  INFO 17064 --- [   scheduling-1] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 3
2023-10-13 11:08:39.044  INFO 17064 --- [   scheduling-7] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 4
2023-10-13 11:08:39.044  INFO 17064 --- [   scheduling-7] com.slm.task.task.PaymentSyncTask        : 固定每3S更新报表 5
2023-10-13 11:08:43.050  INFO 17064 --- [   scheduling-4] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
2023-10-13 11:08:48.051  INFO 17064 --- [   scheduling-4] com.slm.task.task.PaymentSyncTask        : 拉取银行支付状态信息……
```

可以看到更新报表任务的超长耗时并没有阻塞到拉取支付状态任务，但更新报表任然存在阻塞，这是 fixedRate 模式的特点。

### 动态定时任务

在 Spring框架中，创建定时任务通常有两种方式：使用 ***@Scheduled*** 注解和实现 ***SchedulingConfigurer*** 接口。@Scheduled 注解的限制在于它的定时时间是静态的，一旦设定就无法更改，除非重新启动服务。相比之下，SchedulingConfigurer 接口提供了更大的灵活性，允许从数据库动态获取定时任务的执行周期，从而实现定时任务的动态调整。

**定时任务源码解析**

从开启定时任务的注解 ***@EnableScheduling*** 开始

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingConfiguration.class)
@Documented
public @interface EnableScheduling {

}
```

导入了 SchedulingConfiguration 类，内部注册了 ScheduledAnnotationBeanPostProcessor

![image-20240925144156222](assets/image-20240925144156222.png)

该类也是一个 BeanPostProcessor，实现了 postProcessAfterInitialization 方法，在这里实现了对 Bean 内标注 @Scheduled 注解的方法扫描并执行 processScheduled 方法将任务注册到 ScheduledTaskRegistrar 中

**ScheduledTaskRegistrar**

> Helper bean for registering tasks with a TaskScheduler（via Spring-context-5.2.9.RELEASE）
>
> 用于向 TaskScheduler 注册任务的 Helper bean

内部有多种用于注册任务的方法，例如 @Scheduled 可配置的 CronTask、FixedDelayTask、FixedRateTask，也可以自行添加任务

```java
public void addTriggerTask(Runnable task, Trigger trigger) {
	addTriggerTask(new TriggerTask(task, trigger));
}
public void addTriggerTask(TriggerTask task) {
    if (this.triggerTasks == null) {
        this.triggerTasks = new ArrayList<>();
    }
    this.triggerTasks.add(task);
}
```

该类除了是任务的注册器，还是实际推送至执行线程池的推手，在 InitializingBean.afterPropertiesSet 接口实现中将任务提交给了 TaskScheduler，如果没有给定 taskScheduler，在随送任务之前会创建一个单线程定时任务执行器，这也是为什么 Spring Boot 定时任务默认是同步的原因！

下一步，ScheduledAnnotationBeanPostProcessor 会触发 ContextRefreshedEvent 监听，在 refresh 阶段完成后进行 SchedulingConfigurer 自定义实现类的注册和 TaskScheduler 的获取。

**SchedulingConfigurer**

定时任务配置器，有且仅有一个方法 `void configureTasks(ScheduledTaskRegistrar taskRegistrar)` 用于配置任务。得益于 ScheduledTaskRegistrar 注册任务的方法入参均为接口，可以允许我们在传入时自定义实现接口，在此期间可以实时获取来此外层的数据

```java
@Value("${application.task.dynamicCron}")
private String cron;
@Override
public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    // 动态使用cron表达式设置循环间隔
    taskRegistrar.addTriggerTask(new Runnable() {
        @Override
        public void run() {
            log.info("Current time： {}", LocalDateTime.now());
        }
    }, new Trigger() {
        @Override
        public Date nextExecutionTime(TriggerContext triggerContext) {
            // 使用CronTrigger触发器，可动态修改cron表达式来操作循环规则
            CronTrigger cronTrigger = new CronTrigger(cron);
            Date nextExecutionTime = cronTrigger.nextExecutionTime(triggerContext);
            return nextExecutionTime;
        }
    });
}
```

当上述方法中的 Trigger.nextExecutionTime 被触发时，会获取实际的 cron 数值以实现动态配置！

