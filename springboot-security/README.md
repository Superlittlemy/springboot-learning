# springboot-security

Spring Boot Security 安全框架

Spring Security 是一个功能强大且高度可定制的身份验证和访问控制框架，同时也是 Spring Boot 官方推荐使用的安全框架。

Spring Security 的核心是提供认证（Authentication）、授权（Authorization）和攻击防护。

- **认证（Authentication）**：验证试图访问特定资源的人的身份，即你是谁。

- **授权（Authorization）**：允许谁访问什么资源，即你能干什么
- **攻击防护**：防止伪造身份

Spring Security 核心是通过自动装配组成过滤器链，逐步完成认证和授权，发现异常则抛给异常处理器处理

![](.\14483918-baacf316904e06b7.webp)

1）集成 Spring Security

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

此时启动就算是集成了 Spring Security，日志里会生成一串密码，此时访问接口会跳转至自带的登录页面，默认账号user的密码就是日志里的那串长密码。

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

3）配置登录成功/失败处理器

实现抽象登录成功/失败处理接口

```java
@Slf4j
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException, ServletException {
        log.info("用户 " + auth.getName() + " 登录成功");
    }

}
```

```java
@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        if (e instanceof AccountExpiredException) {
            // 账号过期
            log.info("[登录失败] - 用户账号过期");
        } else if (e instanceof BadCredentialsException) {
            // 密码错误
            log.info("[登录失败] - 用户密码错误");
        } else if (e instanceof CredentialsExpiredException) {
            // 密码过期
            log.info("[登录失败] - 用户密码过期");
        } else if (e instanceof DisabledException) {
            // 用户被禁用
            log.info("[登录失败] - 用户被禁用");
        } else if (e instanceof LockedException) {
            // 用户被锁定
            log.info("[登录失败] - 用户被锁定");
        } else {
            // 其他错误
            log.error(String.format("[登录失败] - [%s]其他错误"), e);
        }
    }

}
```

配置处理器

```
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                // 除上面外的所有请求全部需要鉴权认证
                .anyRequest().authenticated()
                .and()
                .formLogin()
                // 登录成功处理逻辑
                .successHandler(loginSuccessHandler)
                // 默认成功页面，第三参数如果为true，登录成功会固定调转该页面
                .defaultSuccessUrl("/index.html", true)
                // 登录失败处理逻辑
                .failureHandler(loginFailureHandler)
                .permitAll()
                // 关闭 csrf 防御
                .and().csrf().disable();
    }
}
```

4）自定义登录页面及注销

login.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>登录页面</title>
</head>
<body>
<form action="/login" method="post">
    <table style="width: 255px; margin: 0 auto;">
        <tr>
            <td>用户名：</td>
            <td><input type="text" name="username" /></td>
        </tr>
        <tr>
            <td>密码：</td>
            <td><input type="password" name="password" /></td>
        </tr>
        <tr>
            <td></td>
            <td style="text-align: right">
                <input type="submit" value="提交">
            </td>
        </tr>
    </table>
</form>
</body>
</html>
```


index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>首页</title>
    <link rel="stylesheet" href="element-ui@2.15.13.css">
    <script type="text/javascript" src="vue@2.7.14.js"></script>
    <script type="text/javascript" src="element-ui@2.15.13.js"></script>
    <script type="text/javascript" src="axios@v1.3.3.js"></script>
</head>
<body>
<div id="app">
    <span v-if="!isLogin">未登录</span>
    <el-button type="primary" size="small" v-if="!isLogin" @click="toLoginPage">登录</el-button>
    <span v-if="isLogin">{{ currentUser }}</span>
    <el-button type="primary" size="small" v-if="isLogin" @click="logout">注销</el-button>
</div>
</body>
<script>
    new Vue({
        el: "#app",
        data() {
            return {
                isLogin: false,
                currentUser: ''
            }
        },
        mounted() {
            this.getCurrentUser();
        },
        methods: {
            toLoginPage() {
                location.href = "/login.html";
            },
            getCurrentUser() {
                axios.get('/auth/current-user').then(res => {
                    if (res.status === 200 && !!res.data) {
                        this.isLogin = true;
                        this.currentUser = res.data;
                    } else {
                        this.isLogin = false;
                        this.currentUser = '';
                    }
                }).catch(err => {
                    console.log(err);
                })
            },
            logout() {
                axios.post('/logout').then(res => {
                    location.reload();
                }).catch(err => {
                    console.log(err);
                })
            }
        }
    })
</script>
</html>
```

静态资源放过认证

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/static/**")
                .antMatchers("/index.html")
                .antMatchers("/fronts/**")
                .antMatchers("/*.js")
                .antMatchers("/*.css");
    }
    
}
```

注销处理器

```java
@Slf4j
@Component
public class MyLogoutSuccessHandler implements LogoutSuccessHandler{

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException, ServletException {
        log.info(auth.getName() + " 注销成功");
    }

}
```

追加认证配置

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        // 放行接口
        .antMatchers("/common/**").permitAll()
        .antMatchers("/auth/**").permitAll()
        // 除上面外的所有请求全部需要鉴权认证
        .anyRequest().authenticated()
        .and()
        .formLogin()
        // 登录接口
        .loginProcessingUrl("/login")
        // 登录页面
        .loginPage("/login.html")
        // 登录成功处理逻辑
        .successHandler(loginSuccessHandler)
        // 默认成功页面，第三参数如果为true，登录成功会固定调转该页面
        .defaultSuccessUrl("/index.html", true)
        // 登录失败处理逻辑
        .failureHandler(loginFailureHandler)
        .permitAll()
        .and()
        .logout()
        // 注销接口
        .logoutUrl("/logout")
        .logoutSuccessHandler(logoutSuccessHandler)
        // 注销后删除 cookies
        .deleteCookies("JSESSIONID")
        .permitAll()
        // 关闭 csrf 防御
        .and().csrf().disable();
}
```

