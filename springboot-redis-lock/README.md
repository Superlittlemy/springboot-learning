# springboot-redis-lock

Spring Boot Redis 分布式锁

**什么是分布式锁**

在很多场景中，我们为了保证数据的最终一致性，需要很多的技术来支持，比如分布式事务、分布式锁等。

分布式锁前先讲下其余两种锁：线程锁和进程锁

1）线程锁：主要用于给代码块加锁，在同一时刻仅有一个线程执行该代码块。线程锁仅在同一 JVM 内有效。

2）进程锁：控制操作系统中多个进程访问共享资源，因为进程互相独立，所以不能通过代码块加锁来实现资源互斥。

分布式锁其实就是控制分布式系统不同进程共同访问共享资源的一种锁技术，如果不同的系统或同一系统的不同主机之间共享某一资源，需要互斥来防止彼此的干扰以达到一致性。

分布式锁的特征：

- 互斥：任意时刻，只能由一个客户端获得锁
- 超时释放：获得锁客户端的异常退出可能无法解锁，这时候需要过期时间来自动释放
- 安全性：锁只能被持有的客户端删除
- 可重入性：线程内可多次请求加锁
- 高可用：高频业务下尽可能的快速加解锁，保证服务的稳定及容错

**为什么选择 Redis**

1）实现简单

2）Redis 是单线程的

3）数据结构丰富且 value 能存储 1G 数据

4）满足高可用：极高的读写性能、主从模式、支持数据持久化与重启恢复

**Redis 分布式锁原理**

加锁：基于 setnx 命令，保证只有一个人可以拿到锁

解锁：del 删除锁即可

阻塞：通过循环的尝试 setnx 设置来获取锁，无法获取且未超时则继续循环阻塞

💥存在的问题以及解决方案

**问题1**：程序的异常退出会导致无法触发解锁动作，导致其他人永远无法获得锁，从而产生死锁

解决办法：为锁添加超时时间（expire），此时 setnx 和 expire 命令是分开执行的，无法保证原子性，为了解决这个问题，框架提供了 **setIfAbsent** 函数，可以在设置键值的同时设置过期时间。

**问题2**：线程执行时间超过键过期时间，导致过期后其他线程获得锁无法保证资源的互斥

解决办法：为键过期时间续期，俗称看门狗

**问题3**：保证锁只能被自己释放

解决办法：不管是处于业务内部的错误还是问题2导致的异常获取锁，被谁锁定的资源就应该由谁释放，Redis key 标注了锁的名称，可以在 value 内为每个加锁请求添加唯一 id，每个线程解锁前判断是否为自己。和问题1一样使用 get 和 del 命令无法保证原子，此处也没有函数提供支持，可以借助 lua 命令，具体实现如下：

```java
public Long unlock(String key, String value) {
    String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
    return this.redisTemplate.execute(redisScript, Collections.singletonList(key), value);
}
```

**订单场景实战**

模拟下单

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final static String ORDER_LOCK = "order_lock";
    private AtomicReference<BigDecimal> amount = new AtomicReference<>(new BigDecimal("200"));
    private final RedisUtil redisUtil;

    @SneakyThrows
    public void createOrder(BigDecimal a, int executeTime) {
        // 请求ID
        String requestId = UUID.randomUUID().toString();
        // 尝试获取锁
        int time = 0;
        while (!redisUtil.tryLock(ORDER_LOCK, requestId, 10) && time < 50) {
            Thread.sleep(100);
            time++;
        }
        if (time > 49) {
            log.error("订单超时");
            return;
        }
        log.info(requestId + ": get lock");
        // 业务执行
        if (a.compareTo(amount.get()) > 0) {
            // unlock
            redisUtil.unlock(ORDER_LOCK, requestId);
            throw new RuntimeException("余额不足");
        }
        amount.set(amount.get().subtract(a));
        Thread.sleep(executeTime);
        log.info(requestId + ": created order, sale " + a + " remain: " + amount.get());
        // 解锁
        redisUtil.unlock(ORDER_LOCK, requestId);
    }

}
```

Redis 工具类

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 尝试获取锁
     *
     * @param key
     * @param value
     * @param expireTime 过期时间
     * @return
     */
    public Boolean tryLock(String key, Object value, long expireTime) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("尝试锁对象失败", e);
            return false;
        }
    }

    /**
     * 解锁
     * 需要根据value判断当前获得锁的是否为本线程生成的requestId，否则不解除别人的锁
     *
     * @param key
     * @param value
     */
    public Long unlock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        return this.redisTemplate.execute(redisScript, Collections.singletonList(key), value);
    }

}
```

测试

```java
@Slf4j
@SpringBootTest
public class BatchOrder {

    @Autowired
    private OrderService orderService;

    @Test
    void batchOrder() {
        new Thread(() -> {
            while (true) {
                try {
                    orderService.createOrder(new BigDecimal("10"), 100);
                    Thread.sleep(300);
                } catch (Exception e) {
                    log.error("想要10个商品，余额不足");
                    break;
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                try {
                    orderService.createOrder(new BigDecimal("20"), 100);
                    Thread.sleep(300);
                } catch (Exception e) {
                    log.error("想要20个商品，余额不足");
                    break;
                }
            }
        }).start();
        while (true) {}
    }

}
```

