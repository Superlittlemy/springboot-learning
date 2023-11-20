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

继承 JpaRepository 接口实现了基本的 CURD 功能！

---

### 主键生成策略

JPA 使用 `@Id` 标注字段为主键，生成方式分为两种：一种是依据数据库特性生成，一种是依据代码逻辑生成，分别用到了以下两个注解：

@GeneratedValue：设置主键的生成策略，依赖于具体的数据库

属性：

- strategy：指定 OpenJPA 容器自动生成实体标识的方式

  | 值                      | 说明                       |
  | ----------------------- | -------------------------- |
  | GeneratorType.AUTO      | 主键由程序控制             |
  | GenerationType.IDENTITY | 主键自增                   |
  | GenerationType.SEQUENCE | 使用序列                   |
  | GenerationType.TABLE    | 使用数据库某表字段作为主键 |

  主流数据库支持情况

  | 数据库     | 支持的策略                                                   |
  | ---------- | ------------------------------------------------------------ |
  | MySQL      | GeneratorType.AUTO<br />GenerationType.IDENTITY<br />GenerationType.TABLE<br />**不支持 GenerationType.SEQUENCE** |
  | Oracle     | GeneratorType.AUTO<br />GenerationType.SEQUENCE<br />GenerationType.TABLE<br />**不支持 GenerationType.IDENTITY** |
  | PostgreSQL | GeneratorType.AUTO<br />GenerationType.IDENTITY<br />GenerationType.SEQUENCE<br />GenerationType.TABLE<br />**均支持** |

- generator：定义实体主键生成器的名称

  需要配合 `@GenericGenerator` 使用，在 @GenericGenerator 中定义主键生成器名称及策略

  示例：

  ```java
  @Id
  @GenericGenerator(name = "jpa-uuid", strategy = "uuid")
  @GeneratedValue(generator = "jpa-uuid")
  private String id;
  ```

  在源码 org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory 内定义了默认的几种策略

  ```java
  public DefaultIdentifierGeneratorFactory() {
      register( "uuid2", UUIDGenerator.class );
      register( "guid", GUIDGenerator.class );			// can be done with UUIDGenerator + strategy
      register( "uuid", UUIDHexGenerator.class );			// "deprecated" for new use
      register( "uuid.hex", UUIDHexGenerator.class ); 	// uuid.hex is deprecated
      register( "assigned", Assigned.class );
      register( "identity", IdentityGenerator.class );
      register( "select", SelectGenerator.class );
      register( "sequence", SequenceStyleGenerator.class );
      register( "seqhilo", SequenceHiLoGenerator.class );
      register( "increment", IncrementGenerator.class );
      register( "foreign", ForeignGenerator.class );
      register( "sequence-identity", SequenceIdentityGenerator.class );
      register( "enhanced-sequence", SequenceStyleGenerator.class );
      register( "enhanced-table", TableGenerator.class );
  }
  ```

### **自定义主键生成策略**

实现接口 org.hibernate.id.IdentifierGenerator

```java
public class MId implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return null;
    }

}
```

可以在这里通过分布式中间件，如 Redis 实现分布式唯一主键

### 多对多关联关系

@ManyToOne：通过 @JoinColumn 注明关联的外键字段

```java
public class Student {
    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;
}
```

@OneToMany

方法一：双向关联，需要列表内对象内有该对象，通过 mappedBy 字段标识。CascadeType.PERSIST 表示在持久化的级联操作，也就是保存学校的时候可以一起保存学生，**而 CascadeType.ALL 带来的级联删除可能并不是你想要的**！

```java
public class School {
    @OneToMany(mappedBy="school"，cascade = CascadeType.PERSIST)
    private List<Student> students;
}
```

方法二：中间表关联，借助 @JoinTable 标注中间表及互联字段

```java
 @JoinTable(name = "school_owned_students",
            inverseJoinColumns = {@JoinColumn(name = "student_id", referencedColumnName = "id")},
            joinColumns = {@JoinColumn(name = "school_id", referencedColumnName = "id")},
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), 
            inverseForeignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
private List<Student> students;
```

@OneToOne

可添加注解后直接使用，如字段名特殊，可以添加 @JoinColumn 注明，参考 @ManyToOne 使用方法

@ManyToMany

多对多大多使用中间表实现，可以参考 @OneToMany 使用方法

### 复杂查询 QueryDSL

1）添加依赖，SpringBoot项目可以直接集成，无需配置版本

```xml
<!-- QueryDSL -->
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-apt</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
</dependency>
```

2）Repository 继承 QuerydslPredicateExecutor<>

```java
@Repository
public interface MerchandiseListViewRepository extends JpaRepository<MerchandiseListView, String>,QuerydslPredicateExecutor<MerchandiseListView> {
}
```

父类的 `Page<T> findAll(Predicate predicate, Pageable pageable);` 可以实现类似 Jpa Specification 的复杂查询功能

3）Predicate 生成

方式一（简单集成）：

Controller 层查询接口添加搜索条件，使用 `@QuerydslPredicate` 注解，属性 root 注明过滤的 Entity 是哪个

```java
@GetMapping
@Operation(summary = "查询用户列表")
public ApiResponse<Page<User>> query(@QuerydslPredicate(root = User.class) Predicate predicate,
                                     Pageable pageable) {
    return ApiResponse.ok(userService.pageQuery(predicate, pageable));
}
```

Service 层直接调用 Repository 层的 `Page<T> findAll(Predicate predicate, Pageable pageable)` 即可

自动适配的 Predicate 只支持精确查询，如果需要实现 like 查询需要自定义

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {

    @Override
    default void customize(QuerydslBindings bindings, QUser qUser) {
        bindings.bind(qUser.name).first(StringExpression::contains);
        bindings.excluding(qMerchandiseListView.id);
    }

}
```

另外，QEntity 的对象较大，如果需要明确 DSL 覆盖的字段范围，可以通过限制不需要的过滤字段，添加自定义过滤来实现完整的自动化复杂查询。此处需要注意，屏蔽 DSL 查询字段的注解是 `@QueryType(PropertyType.NONE)` 而非字面的 `@QueryTransient`。

```java
@QueryType(PropertyType.NONE)
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createdDate;
```

方式二（复杂查询）：

配置 JPAQueryFactory Bean

```java
@Configuration
@RequiredArgsConstructor
public class QueryDSLConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

}
```

通过 QEntity 生成更加面向对象的 DSL 复杂查询

```java
@Service
@RequiredArgsConstructor
public class MerchandiseServiceImpl implements MerchandiseService {

    private final MerchandiseRepository merchandiseRepository;
    private final MerchandiseListViewRepository merchandiseListViewRepository;
    private final JPAQueryFactory jpaQueryFactory; // ADD

    @Override
    public Page<MerchandiseListView> list(Predicate predicate, Pageable pageable) {
        QMerchandiseListView qm = QMerchandiseListView.merchandiseListView;
        List<MerchandiseListView> data = jpaQueryFactory.selectFrom(qm).where(predicate).fetch();
        long total = jpaQueryFactory.select(qm.id.count()).from(qm).where(predicate).fetchCount();
        return new PageImpl<>(data, pageable, total);
    }
}
```

**PS：找不到QEntity 问题**

添加 apt 编译插件解决

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>com.mysema.maven</groupId>
            <artifactId>apt-maven-plugin</artifactId>
            <version>1.1.3</version>
            <executions>
                <execution>
                    <goals>
                        <goal>process</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>target</outputDirectory>
                        <processor>com.querydsl.apt.jpa.JPAAnnotationProcessor</processor>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 投影 Projections

Spring Data 数据查询方法的返回通常的是 Repository 管理的聚合根的一个或多个实例。但是，有时候我们只需要返回某些特定的属性，不需要全部返回，或者只返回一些复合型的字段。Spring Data 允许我们对特定的返回类型建模，以便更有选择的检索托管聚合的部分视图。

#### 基于接口的投影

##### 闭合投影（Closed Projections）

一个投影接口，其 get 方法都与实体类的属性相同，被认为是一个闭合投影。如果使用闭合投影 Spring Data 可以优化查询执行，因为我们知道支持投影代理所需要的所有属性，实际执行 SQL 查询的字段会被优化为接口内列明的字段。

```java
public interface UserVO {

    String getName();
    Gender getGender();
    LocalDateTime getCreatedDate();

}
```

##### 开放投影（Open Projections）

投影接口中的 get 方法也可以使用 @Value 注释计算新值。

```java
public interface UserVO {

    String getName();
    Gender getGender();
    @Value("#{target.gender.getDesc()}")
    String getGenderName();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getCreatedDate();

}
```

支持嵌套投影，可以在投影接口内集成其他投影接口，字段会被识别到下层投影

```java
public interface GenderVO {

    // 注意：此处 target 为根据嵌入的方法名称识别到的 gender 字段
    @Value("#{target.name()}")
    String getCode();
    @Value("#{target.getDesc()}")
    String getName();

}
public interface UserVO {

    String getName();
    GenderVO getGender();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getCreatedDate();

}
```

更加强大的映射功能

方法一：基于 JAVA 8 的接口默认方法特性实现

```java
public interface UserVO {

    String getName();
    Gender getGender();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getCreatedDate();

    default String getGenderName() {
        return getGender() == null ? null : getGender().getName();
    }

}
```

方法二：基于 Spring Bean 的方法实现（可以使用 Spring 管理的持久化和工具类等）

```java
public interface UserVO {

    String getName();
    Gender getGender();
    @Value("#{XXXBean.getGenderName(target.gender)}")
    String getGenderName();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getCreatedDate();

}
@Component
class XXXBean {

	public String getGenderName(Gender gender) {
		return gender == null ? null : gender.getName();
	}

}
```

#### 基于类的投影DTO

定义投影的另一种方是使用值类型DTO（数据传输对象），该 DTO 持有需要检索的属性。DTO 投影的使用方式与接口投影完全相同，只是不会发生代理，也不能用嵌套投影。要加载的字段由公开的构造方法的参数名确定。使用 lombok 的 @Value 注解来简化 DTO 编写。

```java
import lombok.Value;

@Value
public class UserDTO {

    String name;
    Gender gender;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdDate;

}
```

#### 动态投影

Repository方法改造为如下：

```java
/**
  *  动态返回投影，type可以是实体，接口投影，DTO投影
  */
<T> T findById(Long id, Class<T> type);
```

调用时，动态确定返回投影：

```java
@Test
void findById(){
    // 返回实体
    User user = userRepository.findById(1, User.class);

    System.out.println("===================");

    // 返回接口投影
    UserVO user2 = userRepository.findById(1, UserVO.class);

    System.out.println("===================");

    //返回DTO投影
    UserDTO user3 = userRepository.findById(1, UserDTO.class);
}
```

其余功能：

- 支持分页排序
- 支持 Optional 返回
- 支持与 @Query 配置查询（**如果投影对象为 DTO，需要全限定名构造函数接收**）

via https://www.cnblogs.com/caofanqi/p/11924299.html

---

### 各细节部分解决方案清单

**清单1**：数据库默认值

```java
@Column(insertable = false)
private Boolean deleted;
```

数据库该字段为非空，默认 false

**清单2**：逻辑删除及自动过滤

```java
@Data
@Entity
@Where(clause = "deleted != 1")
@SQLDelete(sql = "update user set deleted = 1 where id=?")
@EntityListeners(AuditingEntityListener.class)
public class User {
}
```

- @Where：查询该对象时默认添加的过滤条件
- @SQLDelete：替代 JPA 默认的 delete SQL 语句

**清单3**：字段枚举

```java
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) // ADD
    private Gender gender;

}
```

需要添加依赖支持：

```xml
<dependency>
    <groupId>com.vladmihalcea</groupId>
    <artifactId>hibernate-types-55</artifactId>
    <version>2.19.2</version>
</dependency>
```

如果数据库类型为 PostgreSQL 还需要额外添加以下注解：

```java
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class) // ADD
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Type(type = "pgsql_enum") // ADD
    @Enumerated(EnumType.STRING)
    private Gender gender;

}
```

**清单4**：默认值自动插入

在类上添加注解 @DynamicInsert，可以在执行 insert 时将为 null 的字段排除。此方法可以解决数据库约束了字段非空时的 insert 语句错误问题。

```java
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
public class User {
}
```