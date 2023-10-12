# springboot-admin

Spring Boot Admin 监控

Spring Boot Admin 可以监控 Spring Boot 单机或集群项目，它提供详细的健康 （Health）信息、内存信息、JVM 系统和环境属性、垃圾回收信息、日志设置和查看、定时任务查看、Spring Boot 缓存查看和管理等功能。

![](D:\workspace\practice-master\springboot-master\code\springboot-admin\Snipaste_2023-10-12_17-07-55.png)

**搭建监控端**

1）添加依赖

```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>2.2.0</version>
</dependency>
```

2）在启动类添加 `@EnableAdminServer` 即可

**客户端接入监控**

1）添加依赖

```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-client</artifactId>
    <version>2.2.0</version>
</dependency>
```

2）配置监控端参数

```yaml
spring:
  application:
    name: springboot-properties
  boot:
    admin:
      client:
        url: http://localhost:8080
management:
  endpoints:
    web:
      exposure:
        include: '*'
```

