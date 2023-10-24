# springboot-jpa

Spring Boot JPA - 全自动 ORM

**JPA**

- Jakarta Persistence API：对象持久化 API

- Java EE 5.0 平台标准的 ORM 规范，是的应用程序以统一的方式访问持久层

它是一套 ORM 规范，而不是具体的实现。

**JPA 与 Hibernate**

由于它们相互交织的历史，Hibernate 和 JPA 经常被混为一谈。但是，与 Java Servlet 规范一样，JPA 催生了许多兼容的工具和框架。Hibernate 只是众多 JPA 工具中的一种。

**什么是 ORM？**

ORM，即对象关系映射，在每个 JPA 实现中都提供了某种 ORM 层用来实现 Java 类与对象的转换，以便于在关系数据库存储与管理。从编程角度讲，ORM 层是一个适配器层，用以适应 SQL 与关系表的语言，当你想要存储和检索对象时，只需要调用 JPA 来实现即可，应用程序与数据库之间大部分连接将由 JDBC 处理。

**Spring Data JPA**

Spring Data JPA 为 Jakarta Persistence API（JPA）提供 repository 支持。它简化了需要访问JPA数据源的应用程序的开发。

**实体状态**

实体是对象的总称，其持久性与 ORM 映射。正在运行的应用程序中的实体将始终处于四种状态之一：瞬态、托管、游离和删除。

任何不持久的对象都是**瞬态**的。该对象此时只是一个潜在的实体。一旦调用了 entityManager.persist() ，它就变成了一个持久实体。

**托管**对象是一个持久实体，JPA 使用代理的方式使查询到的同一个实体始终是同一个对象，持久的表现为：对象属性 set，同样能在事物提交后同步至数据库。

当事务提交后，处于托管状态的对象就转变为了**游离**状态。此时该对象已经不处于持久化上下文中，因此任何对于该对象的修改都不会同步到数据库中。

当一个实体已从数据存储中删除，但仍作为活动对象存在时，我们称其处于已**删除**状态。

![](D:\workspace\practice-master\springboot-master\code\springboot-jpa\97b62b3b-9a50-302e-8180-c765e30eb841.jpg)

**Hello World**

1）添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

2）配置数据源

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/bsp
    username: root
    password: 123456
```

3）业务相关

User.class

```java
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;
    private Boolean deleted;

}
```

UserRepository.class

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
}
```

继承 JpaRepository 接口实现了基本的 CURD 功能