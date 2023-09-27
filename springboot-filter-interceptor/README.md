# springboot-filter-interceptor

Spring Boot 过滤器和拦截器

**过滤器 filter**

过滤器主要用于对用户请求进行预处理，也可以对响应进行后处理。

过滤器应用场景

- 过滤黑名单
- 过滤敏感词汇
- 设置字符编码，对非标准编码的请求解码
- 压缩响应信息

**拦截器 interceptor**

拦截器是一种面向切面编程（AOP），可以将通用服务进行分离，可以在不侵入业务代码的情况下干预业务代码执行，甚至终止它！

拦截器应用场景

- 登录验证
- 权限验证
- 日志记录

**过滤器与拦截器的区别**

- 过滤器是基于函数回调的（职责链），而拦截器则是基于 Java 反射的；
- 过滤器依赖于 Servlet 容器，而拦截器不依赖于 Servlet 容器；
- 过滤器对几乎所有的请求起作用，而拦截器只能对 Action 请求起作用；
- 拦截器可以访问 Action 的上下文，值栈里的对象，而过滤器不能；
- 在 Action 的生命周期里，拦截器可以被多次调用，而过滤器只能在容器初始化时调用一次。

**执行顺序**

过滤前 -> 拦截前 -> 业务 -> 拦截后 -> 请求与响应完成 -> 过滤后



过滤器示例

```java
/**
 * IP过滤器
 */
@Component
public class SensitiveFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if ("0:0:0:0:0:0:0:1".equals(request.getRemoteHost()) || "127.0.0.1".equals(request.getRemoteHost())) {
            response.getWriter().append("Not Allowed Ip!");
        } else {
            filterChain.doFilter(request, response);
        }
    }

}

############################
接口返回
Not Allowed Ip!
```

拦截器示例

```java
/**
 * 登录拦截器
 */
@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("拦截前");
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("拦截后");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("请求与相应完成");
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

}

/**
 * 注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor()).addPathPatterns("/**");
    }

}

############################
拦截日志
2023-09-27 10:48:39.229  INFO 20176 --- [nio-8080-exec-1] c.f.interceptor.LogInterceptor           : 拦截前
2023-09-27 10:48:39.256  INFO 20176 --- [nio-8080-exec-1] c.f.interceptor.LogInterceptor           : 拦截后
2023-09-27 10:48:39.257  INFO 20176 --- [nio-8080-exec-1] c.f.interceptor.LogInterceptor           : 请求与相应完成
```

