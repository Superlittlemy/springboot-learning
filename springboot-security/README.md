# springboot-security

Spring Boot Security 安全框架

Spring Security 是一个功能强大且高度可定制的身份验证和访问控制框架，同时也是 Spring Boot 官方推荐使用的安全框架。

Spring Security 的核心是提供认证（Authentication）、授权（Authorization）和攻击防护。

- **认证（Authentication）**：验证试图访问特定资源的人的身份，即你是谁。

- **授权（Authorization）**：允许谁访问什么资源，即你能干什么
- **攻击防护**：防止伪造身份

Spring Security 核心是通过自动装配组成过滤器链，逐步完成认证和授权，发现异常则抛给异常处理器处理

![](.\14483918-baacf316904e06b7.webp)

### 默认实现

1）集成 Spring Security

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

此时启动就算是集成了 Spring Security，日志里会生成一串密码，此时访问接口会跳转至自带的登录页面，默认账号 user 的密码就是日志里的那串长密码。

2）获取自定义用户密码

配置密码解析器

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

实现 UserDetailsService 接口自定义获取账号密码的方式，通常为数据库查询（示例代码采用静态账号密码方式）

```java
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        return new User("user", "$2a$10$I2a4XgXFvn0HeV5HiQZuleB3EzxTj3YloTflCTKF98KVepRSAv5tG", Collections.emptyList());
    }

}
```

一个请求过来 Spring Security 会按照下图的步骤处理：

![](D:\workspace\springboot-learning\springboot-security\asserts\v2-7836b6bc78e9b8316bba24696bd734a0_720w.png)

- Filter：拦截Http请求，获取用户名和秘密等认证信息

- AuthenticationManager：从 filter 中获取认证信息，然后查找合适的 AuthenticationProvider 来发起认证流程
- AuthenticationProvider：调用UserDetailsService来查询已经保存的用户信息并与从http请求中获取的认证信息比对。如果成功则返回，否则则抛出异常
- UserDetailsService：负责获取用户保存的认证信息，例如查询数据库

这些组件都是抽象的，每个都可以有不同的实现，换句话说都是可以定制，特别灵活，所以就特别复杂。具体到我们这个默认的例子中，使用的都是默认实现：

- Filter： UsernamePasswordAuthenticationFilter
- AuthenticationManager： ProviderManager
- AuthenticationProvider： DaoAuthenticationProvider
- UserDetailsService： InMemoryUserDetailsManager

### 使用 Token 认证方案

#### JWT

比较流行的就是使用 JWT（JSON Web Tokens），其是一个开放的工业标准。

JWT 由一下三部分组成：Header、Payload 和 Signature

![](D:\workspace\springboot-learning\springboot-security\asserts\13587608-4b9221877778b63e.jpg)

Header 头部分是一个描述JWT元数据的JSON对象，承载了两部分信息：

- 声明类型，如：jwt
- 声明加密的算法，通常使用 HMAC SHA256

```
{
  'typ': 'JWT',
  'alg': 'HS256'
}
```

Payload 载荷就是存放有效信息的地方

- 标准中注册的声明（建议但不强制使用）

  iss: jwt签发者

  sub: jwt所面向的用户

  aud: 接收jwt的一方

  exp: jwt的过期时间，这个过期时间必须要大于签发时间

  nbf: 定义在什么时间之前，该jwt都是不可用的.

  iat: jwt的签发时间

  jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击。

- 公共的声明

  公共的声明可以添加任何的信息，一般添加用户的相关信息或其他业务需要的必要信息.但不建议添加敏感信息，因为该部分在客户端可解密。


Signature 签证信息，这个签证信息由三部分组成：

- header (base64后的)
- payload (base64后的)
- secret

**注意**：secret是保存在服务器端的，jwt的签发生成也是在服务器端的，secret就是用来进行jwt的签发和jwt的验证，所以，它就是你服务端的私钥，在任何场景都不应该流露出去。一旦客户端得知这个secret, 那就意味着客户端是可以自我签发jwt了。

#### 认证流程

1. 登录

   用户使用用户名与秘密登录我们的系统，登录成功后颁发JWT给用户

2. 发起请求

   用户发起请求时在Header中携带JWT，程序拦截并检查这个token是否合法，合法则放行，不合法则提示从新登录。

代码实现

1）引入 JWT 依赖库

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.5</version>
</dependency>
```

2）生成 JWT

```java
/**
 * 创建 JWT
 *
 * @return 返回生成的jwt token
 */
public static String generateJwtToken(String username, String password) {
    return Jwts.builder()
            .header()
            .add("typ", "JWT")
            .add("alg", "HS256")
            .and()
            .claim("username", username)
            // 令牌ID
            .id(UUID.randomUUID().toString())
            // 过期日期
            .expiration(new Date(System.currentTimeMillis() + access_token_expiration * 1000))
            // 签发时间
            .issuedAt(new Date())
            // 主题
            .subject(subject)
            // 签发者
            .issuer(jwt_iss)
            // 签名
            .signWith(KEY, ALGORITHM)
            .compact();
}
```

3）登录接口

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @RequestMapping("login")
    public String login(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(authenticationToken);
        //上一步没有抛出异常说明认证成功，我们向用户颁发jwt令牌
        return JWTUtil.generateJwtToken(username, password);
    }

}
```

4）拦截请求，验证 token

```java
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final static String AUTH_HEADER = "Authorization";
    private final static String AUTH_HEADER_TYPE = "Bearer";

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // get token from header:  Authorization: Bearer <token>
        String authHeader = request.getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(AUTH_HEADER_TYPE)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authToken = authHeader.split(" ")[1];
        log.info("authToken:{}" , authToken);
        // verify token
        Jws<Claims> claims = JWTUtil.getClaimsFromJwt(authToken);
        String username = (String) claims.getPayload().get("username");;
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        // 注意，这里使用的是3个参数的构造方法，此构造方法将认证状态设置为true
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
        //将认证过了凭证保存到security的上下文中以便于在程序中使用
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }

}
```

5）配置 security

```java
@Order(-1)
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    // 配置密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    //我们自定义的拦截器
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/static/**")
                .antMatchers("/index.html")
                .antMatchers("/fronts/**")
                .antMatchers("/*.js")
                .antMatchers("/*.css");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                //基于token，所以不需要session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 放行接口
                .antMatchers("/auth/**").permitAll()
                // 除上面外的所有请求全部需要鉴权认证
                .anyRequest().authenticated()
                // 关闭 csrf 防御
                .and().csrf().disable()
                // 禁用缓存
                .headers().cacheControl();
    }
}
```

此时登录会返回 token，访问接口带着 token 头被拦截器拦截并验证 token 是否存在或是否过期！

参考资料：

- https://zhuanlan.zhihu.com/p/625403750 秒懂SpringBoot之易懂的Spring Security教程
- https://www.jianshu.com/p/576dbf44b2ae 什么是 JWT -- JSON WEB TOKEN
