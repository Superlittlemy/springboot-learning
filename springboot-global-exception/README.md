# springboot-global-exception

Spring Boot 全局异常处理

为了更好的贯彻 RESTful 有求必应的原则以及友好的订单错误，通过统一的异常处理类来汇总处理

**具体实现**

```java
@RestControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

}
```

## RestControllerAdvice

@RestControllerAdvice 是一个组合注解，由 @ControllerAdvice、@ResponseBody 组成。

@ControllerAdvice 注解可以对控制器进行全局控制，而 @ResponseBody 就是以 JSON 对象的形式返回。

## ExceptionHandler

处理局部异常，仅限于 controller 内的异常，如果将该方法标注在 @ControllerAdvice 的类上可以升级为对全局的异常处理。

在注解上标注了什么异常，这个方法就处理什么异常。方法的入参中可以加入Exception 类型的参数，该参数即对应发生的异常的对象。

**可以定义多个 ExceptionHandler，但是不能处理相同的异常**，以下是错误示例：

```java
@ExceptionHandler(Exception.class)
public ApiResponse<String> handler(Exception e) {
    return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
}

@ExceptionHandler(Exception.class)
public ApiResponse<String> handler2(Exception e) {
    return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
}
```

**处理的优先级**

- 异常类型越细化，就用谁

```java
@ExceptionHandler(Exception.class)
public ApiResponse<String> handler(Exception e) {
    return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
}

// IO 异常会直接命中这个处理方法
@ExceptionHandler(IOException.class)
public ApiResponse<String> handler(IOException e) {
    return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
}
```

- 同包不同类，按类顺序
- 不同包不同类，按包顺序
