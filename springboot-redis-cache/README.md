# springboot-redis-cache

Spring Boot Redis 缓存

服务器对数据库的高频重复访问，而这其中可能存在大量重复的短期不会被更改的数据，重复的访问数据库得到的依旧是一样的数据，并且徒增了数据库的压力。**缓存**就是避免这类问题的手段，它可以在数据库外存放既定的数据事实，通过比数据库更快的工具来加速数据查询。

**Redis** 是一款开源的 key-value 数据库，因其丰富的数据结构、超快的响应速度以及远超数据库的 OPS，让其成为缓存的热门选项。

**Spring Cache** 是 Spring 提供的一套缓存解决方案，它不是具体的缓存实现，它只提供接口和代码规范、配置、注解等，用于整合各种缓存方案。

---

**整合 Redis 缓存**

1. 添加依赖

   ```xml
   <dependency>
   	<groupId>org.springframework.boot</groupId>
   	<artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   ```

2. 配置 redis

   ```yaml
   spring:
     redis:
       host: localhost
       database: 0
       port: 6379
       password: 123456
   ```

3. 开启缓存

   > 启动类添加 @EnableCaching 注解

4. 缓存数据

   | 注解        | 说明                                                         |
   | ----------- | ------------------------------------------------------------ |
   | @Cacheable  | 使用该注解的方法当缓存存在时，会从缓存中获取数据而不执行方法，当缓存不存在时，会执行方法并把返回结果存入缓存中。一般使用在**查询**方法上 |
   | @CachePut   | 使用该注解的方法每次执行时都会把返回结果存入缓存中。一般使用在**新增**方法上 |
   | @CacheEvict | 使用该注解的方法执行时会清空指定的缓存。一般使用在**更新**或**删除**方法上 |
   
   PS：前两个注解的 unless 参数可以屏蔽需要过滤的缓存
   
5. 代码实现

   ```java
   import com.slm.rediscache.entity.User;
   import com.slm.rediscache.mapper.UserMapper;
   import com.slm.rediscache.model.UserCreateRequest;
   import com.slm.rediscache.model.UserUpdateRequest;
   import com.slm.rediscache.service.UserService;
   import lombok.RequiredArgsConstructor;
   import org.springframework.cache.annotation.CacheEvict;
   import org.springframework.cache.annotation.CachePut;
   import org.springframework.cache.annotation.Cacheable;
   import org.springframework.stereotype.Service;
   import org.springframework.transaction.annotation.Transactional;
   
   import java.util.List;
   import java.util.Objects;
   
   @Service
   @RequiredArgsConstructor
   public class UserServiceImpl implements UserService {
   
       private final static String REDIS_USER_INFO_KEY= "USER-INFO";
   
       private final UserMapper userMapper;
   
       @Override
       public List<User> query() {
           return userMapper.query();
       }
   
       @Override
       @Cacheable(value = REDIS_USER_INFO_KEY, key = "#id", unless = "#result==null")
       public User get(Long id) {
           return userMapper.get(id);
       }
   
       @Override
       @CachePut(value = REDIS_USER_INFO_KEY, key = "#result.id", unless = "#result==null")
       @Transactional(rollbackFor = Exception.class)
       public User create(UserCreateRequest request) {
           User user = User.builder().name(request.getName()).build();
           userMapper.add(user);
           return user;
       }
   
       @Override
       @CacheEvict(value = REDIS_USER_INFO_KEY, key = "#request.id")
       @Transactional(rollbackFor = Exception.class)
       public void modify(UserUpdateRequest request) {
           User user = userMapper.get(request.getId());
           if (Objects.isNull(user)) {
               throw new RuntimeException("用户不存在");
           }
   
           user.setName(request.getName());
           userMapper.update(user);
       }
   
       @Override
       @CacheEvict(value = REDIS_USER_INFO_KEY, key = "#id")
       @Transactional(rollbackFor = Exception.class)
       public void delete(Long id) {
           User user = userMapper.get(id);
           if (Objects.isNull(user)) {
               throw new RuntimeException("用户不存在");
           }
           if (Boolean.TRUE.equals(user.getDeleted())) {
               throw new RuntimeException("用户已被删除，请刷新");
           }
   
           user.setDeleted(Boolean.TRUE);
           userMapper.update(user);
       }
   
   }
   ```

**Mybatis 打印查询日志**

在 application.yaml 配置如下参数即可：

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

