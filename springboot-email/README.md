# springboot-email

Spring Boot Email 邮件服务

**前期准备**

开启邮箱的 IMAP/SMTP 服务

![](D:\workspace\practice-master\springboot-master\code\springboot-email\Snipaste_2023-10-11_18-11-51.png)

新增授权码

![](D:\workspace\practice-master\springboot-master\code\springboot-email\Snipaste_2023-10-11_18-12-57.png)

**集成邮件服务**

1）添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

2）配置参数

```yaml
spring:
  mail:
    host: smtp.163.com
    username: XXX@163.com
    password: XXX # 授权码
```

2）邮件工具类

```java
@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void send(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        // 收信人
        message.setTo(to);
        // 主题
        message.setSubject(subject);
        // 内容
        message.setText(content);
        // 发信人
        message.setFrom(from);
        javaMailSender.send(message);
    }

}
```

简单的邮件发送就实现啦！

**HTML 邮件**

如果想要邮件版式自己定义，可以使用 HTML 邮件，实现方面，改用 MimeMessage，借助 MimeMessageHelper 提供 HTML 文本支持，所需参数相同。

```java
public void htmlEmail(String to, String subject, String content) throws MessagingException {
    MimeMessage message = javaMailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    // 收信人
    helper.setTo(to);
    // 主题
    helper.setSubject(subject);
    // 内容
    helper.setText(content, true);
    // 发信人
    helper.setFrom(from);
    javaMailSender.send(message);
}
```

结果示例：

![](D:\workspace\practice-master\springboot-master\code\springboot-email\Snipaste_2023-10-12_10-40-05.png)

**添加静态图像**

HTML 载体内部除了使用网络 URL 资源，也可以内嵌静态资源，如图片等。

1）在 HTML 内添加 `<image>` 标签，src 属性使用 cid:resId 形式，resId 是资源的唯一 id。

2）通过 MimeMessageHelper 为邮件关联 HTML 载体内 image 等标签的资源

```java
helper.addInline(id, file);
```

结果示例：

![](D:\workspace\practice-master\springboot-master\code\springboot-email\Snipaste_2023-10-12_11-09-00.png)

**邮件附件**

除了可视信息的传递，有时候文件也是重要的传输载体，可以为各方提供信息传递

使用 `helper.addAttachment(name, file)` 为邮件添加附件

:beetle:特别注意：附件名称可能存在过长截取的情况，从而导致文件名称乱码

:cactus: 解决方案：添加如下代码

```java
System.getProperties().setProperty("mail.mime.splitlongparameters", "false");
```

结果示例：

![](D:\workspace\practice-master\springboot-master\code\springboot-email\Snipaste_2023-10-12_11-24-56.png)
