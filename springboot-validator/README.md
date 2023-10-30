# springboot-validator

Spring Boot 参数校验

## JSR-303

JSR-303 是 JAVA EE 6 中的一项子规范，叫做 Bean Validation。但没有提供实现。Hibernate Validation 是对这个规范的实现，并增加了校验注解如@Email、@Length等。Spring Validation 是对 Hibernate Validation 的二次封装，用于支持 Spring MVC 参数自动校验。

**解决的问题**

在未使用 Bean Validation 之前，在接口层需要对入参对象进行大量的代码校验，从而导致不必要的业务代码使得代码臃肿且不利于维护。

## Hibernate Validation

Hibernate Validator 是 Bean Validation 的参考实现。 Hibernate Validator 提供了 JSR 303 规范中所有内置 constraint 的实现，除此之外还有一些附加的 constraint。

### 常用注解

| 注解                                   | 说明                                                         |
| :------------------------------------- | :----------------------------------------------------------- |
| @NotNull                               | 元素不能为NULL                                               |
| @NotEmpty                              | 元素不能为空，**可以作用于字符串、集合、Map 和 Array**       |
| @NotBlank                              | 字符串元素忽略空格后不能为空                                 |
| @Max(value = XXX)                      | 数字必须小于XXX                                              |
| @Min(value = XXX)                      | 数字必须大于XXX                                              |
| @Range(max = XXX, min =XXX)            | @Max和@Min的合集                                             |
| @Digits(integer = XXX, fraction = XXX) | 浮点数的整数长度和小数位数限制，可用于类型 BigDecimal、BigInteger、字符串以及数字基础类型和它们的包装类 |
| @DecimalMax                            | 效果等同于@Max，但它可以突破 Long 数的最大值上限，因为它的参数是 String 类型的，并且有一个参数 inclusive 可以选择是否包括这个最值 |
| @DecimalMin                            | 与@DecimalMax相反                                            |
| @Size(max = XXX, min = XXX)            | 元素的长度必须在指定值区间（包括边际值），**可以作用于字符串、集合、Map 和 Array** |
| @Length(max = XXX, min = XXX)          | 字符串的长度必须在指定值区间（包括边际值）                   |

### SpringBoot 整合 Hibernate Validator

由于 spring-boot-dependencies 已经集成了 Hibernate Validator，所以只需要引入依赖即可（可以忽略版本）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.4.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <artifactId>springboot-validator</artifactId>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.hibernate.validator</groupId>
            <artifactId>hibernate-validator</artifactId>
        </dependency>
    </dependencies>

</project>
```

为校验类添加需要的注解

```java
@Data
public class UserCreateModel {

    @NotBlank(message = "姓名不能为空")
    private String name;
    @Length(min = 18, message = "二代身份证必须为18位")
    private String idNo;

}
```

为接口开启参数校验

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public void createUser(@RequestBody @Valid UserCreateModel user) {
    }

}
```

## 全局异常处理

结合全局异常处理与前端更优雅的交互

```java
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handler(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder sb = new StringBuilder("参数校验失败:");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append(fieldError.getDefaultMessage()).append("(").append(fieldError.getField()).append("), ");
        }
        return ApiResponse.failure(ResultStatus.ERROR, sb.deleteCharAt(sb.length() - 1));
    }

}

###################################
{
  "code": 50000,
  "message": "失败",
  "data": "参数校验失败：二代身份证必须为18位(idNo)，姓名不能为空(name)"
}
```

