# springboot-properties

springboot 配置文件详解

## @Value

在 Spring Bean 类的属性上添加此注解，使用`${}`包裹配置属性在 application.yaml 中的路径
```
@Value("${spring.application.name}")
private String name;
```
除此之外，还可以使用以下方式注入非配置文件变量
```java
/**
 * 注入普通字符串，相当于直接给属性默认值
 */
@Value("程序新视界")
private String wechatSubscription;

/**
 * 注入操作系统属性
 */
@Value("#{systemProperties['os.name']}")
private String systemPropertiesName;

/**
 * 注入表达式结果
 */
@Value("#{ T(java.lang.Math).random() * 100.0 }")
private double randomNumber;

/**
 * 注入其他Bean属性：注入config对象的属性tool
 */
@Value("#{config.tool}")
private String tool;

/**
 * 注入列表形式（自动根据"|"分割）
 */
@Value("#{'${words}'.split('\\|')}")
private List<String> numList;

/**
 * 注入文件资源
 */
@Value("classpath:config.xml")
private Resource resourceFile;

/**
 * 注入URL资源
 */
@Value("http://www.choupangxia.com")
private URL homePage;
```
缺省默认值
```java
/**
 * 如果属性中未配置ip，则使用默认值
 */
@Value("${ip:127.0.0.1}")
private String ip;
```

## @ConfigurationProperties

  属性配置在 Spring Bean 上， 唯一属性 prefix 或 value，配置属性在配置文件中的前缀，后续属性会通过 set 方法注入，所以必须有 set 方法。

```java
@Component
@ConfigurationProperties(prefix = "properties")
public class Properties {

    private Integer id;
    private String name;
    private String sex;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
    
}
```



  