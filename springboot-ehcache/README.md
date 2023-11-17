# springboot-ehcache

Spring Boot JPA 二级缓存（ehcache）

EhCache 是一个纯 Java 的进程内缓存框架，具有快速、精干的特点。

EhCache 是 Hibernate 中默认的 CacheProvider，Spring Boot 也对其进行了支持，Spring 中提供的缓存抽象也支持对 EhCache 缓存框架的绑定，而且支持基于注解的方式来使用。

EhCache 提供了多种缓存策略，主要分为内存和磁盘两级，是一款面向通用缓存、Java EE和轻量级容器的缓存框架。

**EhCache 与 Redis**

EhCache 直接在 JVM 中进行缓存，速度快，效率高。与Redis相比，操作简单、易用、高效，虽然 EhCache 也提供有缓存共享的方案，但对分布式集群的支持不太好，缓存共享实现麻烦。

Redis 是通过 Socket 访问到缓存服务，效率比 EhCache 低，比数据库要快很多，处理集群和分布式缓存方便，有成熟的方案。

所以，如果是单体应用，或对缓存访问要求很高，可考虑采用 EhCache；如果是大型系统，存在缓存共享、分布式部署、缓存内容很大时，则建议采用 Redis。

**EhCache 实战**

1）引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-ehcache</artifactId>
</dependency>
<dependency>
    <groupId>net.sf.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
```

2）JPA 添加二级缓存支持配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/bsp
    username: root
    password: 123456
  jpa:
    show-sql: true
    properties:
      hibernate:
        cache:
          # 开启查询缓存
          use_query_cache: true
          # 开启二级缓存
          use_second_level_cache: true
          # 缓存工厂
          region:
            factory_class: org.hibernate.cache.ehcache.EhCacheRegionFactory
          # 忽略 ehcache 每个缓存对象都要配置的警告
          ehcache:
            missing_cache_strategy: create
      # 缓存策略
      # ENABLE_SELECTIVE：被@Cacheable显式声明要缓存
      # DISABLE_SELECTIVE：被@Cacheable显式声明不缓存
      # ALL：都支持缓存
      # NONE：不缓存
      javax:
        persistence:
          sharedCache:
            mode: ENABLE_SELECTIVE
```

3）自定义 EhCache 配置

可选，默认使用 net.sf.ehcache:ehcache 包下的 ehcache-failsafe.xml

示例：

```xml
<ehcache>
    <!-- 磁盘存储:将缓存中暂时不使用的对象,转移到硬盘,类似于Windows系统的虚拟内存
     path:指定在硬盘上存储对象的路径-->
    <diskStore path="java.io.tmpdir"/>
    <!--  一、以下属性是必须的：
      　　1、name： Cache的名称，必须是唯一的(ehcache会把这个cache放到HashMap里)。
      　　2、maxElementsInMemory：在内存中缓存的element的最大数目。
      　　3、maxElementsOnDisk：在磁盘上缓存的element的最大数目，默认值为0，表示不限制。
      　　４、eternal：设定缓存的elements是否永远不过期。如果为true，则缓存的数据始终有效，如果为false那么还要根据timeToIdleSeconds，timeToLiveSeconds判断。
      　　５、overflowToDisk： 如果内存中数据超过内存限制，是否要缓存到磁盘上。
      二、以下属性是可选的：
      　　１、timeToIdleSeconds： 对象空闲时间，指对象在多长时间没有被访问就会失效。只对eternal为false的有效。默认值0，表示一直可以访问。
      　　２、timeToLiveSeconds： 对象存活时间，指对象从创建到失效所需要的时间。只对eternal为false的有效。默认值0，表示一直可以访问。
      　　３、diskPersistent： 是否在磁盘上持久化。指重启jvm后，数据是否有效。默认为false。
      　　４、diskExpiryThreadIntervalSeconds： 对象检测线程运行时间间隔。标识对象状态的线程多长时间运行一次。
      　　５、diskSpoolBufferSizeMB： DiskStore使用的磁盘大小，默认值30MB。每个cache使用各自的DiskStore。
      　　６、memoryStoreEvictionPolicy： 如果内存中数据超过内存限制，向磁盘缓存时的策略。默认值LRU，可选FIFO、LFU。
      三、缓存的3 种清空策略 ：
      　　１、FIFO ，first in first out (先进先出).
      　　２、LFU ， Less Frequently Used (最少使用).意思是一直以来最少被使用的。缓存的元素有一个hit 属性，hit 值最小的将会被清出缓存。
      　　３、LRU ，Least Recently Used(最近最少使用). (ehcache 默认值).缓存的元素有一个时间戳，当缓存容量满了，而又需要腾出地方来缓存新的元素的时候，那么现有缓存元素中时间戳离当前时间最远的元素将被清出缓存。-->
    <defaultCache
            maxElementsInMemory="10000"
            eternal="false"
            timeToIdleSeconds="120"
            timeToLiveSeconds="120"
            maxElementsOnDisk="10000000"
            diskExpiryThreadIntervalSeconds="120"
            memoryStoreEvictionPolicy="LRU">
        <persistence strategy="localTempSwap"/>
    </defaultCache>

    <!--
        cache:为指定名称的对象进行缓存的特殊配置
        name:指定对象的完整名
    -->
    <!--   <cache name="sampleCache1"
              maxElementsInMemory="10000"
              eternal="false"
              timeToIdleSeconds="300"
              timeToLiveSeconds="600"
              overflowToDisk="true"
       />-->
</ehcache>
```

4）开启缓存

```java
@EnableCaching
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

5）根据 yaml 配置的 javax.persistence.sharedCache.mode 参数，使用 ENABLE_SELECTIVE 可以按需缓存实体，只需要在表对象上添加 @Cacheable 注解即可，不加则不纳入缓存。

