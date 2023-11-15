# springboot-caffeine

Spring Boot Caffeine 基于 Java8 的高性能缓存库

**特性**

- 自动将条目自动加载到缓存中，可以选择异步加载

- 当超过频率和最近次数的最大值时，基于大小逐出缓存
- 自上次访问或上次写入以来测得的基于时间的条目到期
- 当出现对条目的第一个过时请求时异步刷新
- 键自动包装在弱引用中
- 值自动包装在弱引用或软引用中
- 逐出（或以其他方式删除）条目的通知
- 写入传播到外部资源
- 缓存访问统计信息的累积

**Spring Boot 集成 Caffeine**

1）添加依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context-support</artifactId>
    <version>5.2.9.RELEASE</version>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>2.8.2</version>
</dependency>
```

2）开启缓存

```java
@EnableCaching
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
```

3）配置 CacheManager

```java
@Configuration
public class CacheManagerConfig {

    @Bean
    public CacheManager cacheManager(){
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(128)
                .maximumSize(1024)
                .expireAfterWrite(60, TimeUnit.SECONDS));
        return cacheManager;
    }

}
```

**开发使用**

主要基于 Spring 缓存注解 @Cacheable、@CacheEvict、@CachePut 的方式使用

```java
@Cacheable(value = "XXX", key = "#id")
public Object find(String id) {
	……
}
@CacheEvict(value = "XXX", key = "#id")
public void remove(String id) {
	……
}
```