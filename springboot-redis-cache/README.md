# springboot-redis-cache

Spring Boot Redis 缓存

服务器对数据库的高频重复访问，而这其中可能存在大量重复的短期不会被更改的数据，重复的访问数据库得到的依旧是一样的数据，并且徒增了数据库的压力。**缓存**就是避免这类问题的手段，它可以在数据库外存放既定的数据事实，通过比数据库更快的工具来加速数据查询。

**Redis** 是一款开源的 key-value 数据库，因其丰富的数据结构、超快的响应速度以及远超数据库的 OPS，让其成为缓存的热门选项。

---

**Redis 数据类型**

1）字符串

2）Hash（哈希）

3）List（列表）

4）Set（集合）

5）ZSet（有序集合）

## 基本命令

选择数据库：select 0

查询数据库中 key 的数量：dbsize

### key 相关命令

**语法：keys pattern**

**说明：查找指定 key**

pattern：查询条件

| 模式     | 匹配key                             |
| -------- | ----------------------------------- |
| h?llo    | 匹配 hello, hallo 和 hxllo          |
| h*llo    | 匹配 hllo 和 heeeello               |
| h[ae]llo | 匹配 hello 和 hallo, 不匹配如 hillo |
| `h[^e]llo` | 匹配 hallo, hbllo, ... 不匹配如 hello |
| h[a-e]llo | 匹配 hallo 和 hbllo, [a-e]说明是a~e这个范围 ，如hcllo也可以匹配 |

**语法：del key [key ……]**

**说明：删除指定的 key**

del name age address

**语法：unlink key [key ……]**

**说明：功能与 del 相同，会返回删除的键数量**

unlink name1 name2 name3

PS：del 与 unlink 的区别

- del：是线程阻塞的

- unlink：非线程阻塞的，删除任务会交由另外线程执行（效率比 del 高）

**语法：exists key**

**说明：查询键是否存在**

exists name

**语法：type key**

**说明：返回指定 key 的类型**

type name

**语法：rename key newkey**

**说明：修改 key 名称，如果 newkey 存在则用 key 的值覆盖**

rename name name1

PS：如果不想要重名了覆盖已有键，可以使用 renamenx 命令

**语法：randomkey**

**说明：随机返回一个key名称**

**语法：expire key seconds [nx|xx|gt|lt]**

**说明：为一个存在的key设置过期时间 秒**

options：

- NX：只有当key没有设置过期时间，才会执行命令（已经设置过的，不能再设置）
- XX ：只有当key有过期时间，才会执行命令设置（没有设置过的，不能设置）
- GT ：只有当新的过期时间大于当前过期时间时，才会设置（只会增加过期时间）
- LT ：只有当新的过期时间大于当前过期时间时，才会设置（只会减少过期时间）

PS：expire 过期时间单位为秒，需要使用毫秒可以使用 pexpire 命令

**语法：expireat key timestamp [nx|xx|gt|lt]**

**说明：为一个存在的 key 设置指定时间戳过期**

PS：如果需要精确到毫秒，可以使用 pexpireat 命令

**语法：persist key**

**说明：清除过期时间，如果没有设置过期时间则返回 0**

persist name

**语法：ttl key**

**说明：查看 key 的剩余时间，返回秒**

ttl key

PS：如果需要精确到毫秒，可以使用 pttl 命令

**语法：move key db**

**说明：移动指定 key 到目标数据库**

move name 2

### String 类型命令

**语法：set key value [ex|px|exat|pxat|keepttl|nx|xx]**

**说明：设置 string 类型的键值，如果 key 已存在则覆盖**

options：

- ex = expire：设置过期时间，单位秒

- px = pexpire：设置过期时间，单位毫秒

- exat = expireat：设置指定时间过期，秒级时间戳

- oxat = pexpireat：设置指定时间过期，毫秒时间戳

- keepttl：返回过期时间

- nx：只有 key 不存在才会成功

- xx：只有 key 存在才会成功

**语法：append key value**

**说明：用于为 key 追加值**

append name ' good boy'

**语法：getdel key**

**说明：获取后删除指定 key**

**语法：getset key**

**说明：获取后更新指定 key**

**语法：getrange key start end**

**说明：获取指定范围内的值**

set name zhangsan

getrange name 2 5 -- 返回angs

getrange name 3 -2 -- 返回ngsa

负数代表从后往前数

**语法：incr key**

**说明：值累加1，非整形字符串会报错**

**语法：incrby key increment**

**说明：累加指定步长 increment**

PS：累减使用 decr 关键字，需要步长参数就用 decrby 命令

参考文档：https://www.cnblogs.com/antLaddie/p/15362191.html

## 注解缓存集成

**Spring Cache** 是 Spring 提供的一套缓存解决方案，它不是具体的缓存实现，它只提供接口和代码规范、配置、注解等，用于整合各种缓存方案。

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

