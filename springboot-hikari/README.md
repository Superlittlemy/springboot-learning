# springboot-hikari

Spring Boot 自带 hikari 数据库连接池详解

Hikari 是一位日本开发者创作的数据库连接池，意为“光”，比喻向光一样的速度 [Github 地址](https://github.com/brettwooldridge/HikariCP)

Hikari 最引以为傲的就是它的性能，从作者 Github 上的图中可以看出，Hikari 在 获取和释放 Connection 和 Statement 方法的 OPS 不是一般的高，那是相当的高，碾压其余连接池。

![](D:\workspace\practice-master\springboot-master\code\springboot-hikari\HikariCP-bench-2.6.0.png)

**为什么这么快！**

官网详细地说明了 HikariCP 所做的一些优化，总结如下：

- 字节码精简 ：优化代码，直到编译后的字节码最少，这样，CPU 缓存可以加载更多的程序代码；
- 优化代理和拦截器：减少代码，例如 HikariCP 的 Statement proxy 只有100行代码，只有 BoneCP 的十分之一；
- 自定义的 FastList 代替 ArrayList：避免每次 get 调用都要进行 range check，避免调用 remove 时的从头到尾的扫描；
- 自定义集合类型 ConcurrentBag，提高并发读写的效率；

原文：https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole

**常用参数配置**

| name                  | 描述                 | 默认值  | 非法值重置                                                   |
| --------------------- | -------------------- | ------- | ------------------------------------------------------------ |
| maximum-pool-size     | 连接池最大连接数     | -1      | 如果 maxPoolSize 小于1，则会被重置。当 minIdle<=0 被重置为 DEFAULT_POOL_SIZE 则为10；如果 minIdle>0 则重置为 minIdle 的值 |
| minimum-idle          | 空闲时保持最小连接数 | -1      | minIdle<0 或者 minIdle>maxPoolSize，则被重置为 maxPoolSize   |
| idle-timeout          | 空闲连接存活时间     | 600000  | 如果 idleTimeout+1秒>maxLifetime 且 maxLifetime>0，则会被重置为0（代表永远不会退出）；如果 idleTimeout!=0 且小于10秒，则会被重置为10秒 |
| max-lifetime          | 连接最大生命周期时长 | 1800000 | 如果不等于0且小于30秒则会被重置回30分钟                      |
| connection-timeout    | 连接超时时间         | 30000   | 如果小于250毫秒，则被重置回30秒                              |
| connection-test-query | 连接测试sql          | -       | -                                                            |

示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/xxx
    username: root
    password: 123456
    hikari:
      # 连接池最大连接数
      maximum-pool-size: 10
      # 空闲时保持最小连接数
      minimum-idle: 2
      # 空闲连接存活时间
      idle-timeout: 300000
      # 连接最大生命周期时长
      max-lifetime: 900000
      # 连接超时时间
      connection-timeout: 20000
      # 连接测试sql
      connection-test-query: select 1
```
