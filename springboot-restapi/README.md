# springboot-restapi

Spring Boot RESTful API 架构

> RESTful API 是一种软件架构风格、设计风格，可以让软件更加清晰，更简洁，更有层次，可维护性更好
> 
> 官网地址：https://restfulapi.cn/

## 请求设计

> 设计原则：请求 = 动词 + 宾语

动词

```
GET：   读取（Read）
POST：  新建（Create）
PUT：   更新（Update）
PATCH： 更新（Update），通常是部分更新
DELETE：删除（Delete）
```

- 动词 使用五种 HTTP 方法，对应 CRUD 操作；
- 宾语 URL 应该全部使用名词复数，可以有例外，比如搜索可以使用更加直观的 search；

## 响应设计

```
1xx：相关信息
2xx：操作成功
3xx：重定向
4xx：客户端错误
5xx：服务器错误
```

- 客户端的每一次请求，服务器都必须给出回应。回应包括 HTTP 状态码和数据两部分；

- 客户端请求时，要明确告诉服务器，接受 JSON 格式，请求的 HTTP 头的 ACCEPT 属性要设成 application/json

- 服务端返回的数据，不应该是纯文本，而应该是一个 JSON 对象。服务器回应的 HTTP 头的 Content-Type 属性要设为 application/json；

## SpringBoot 快速实践 RESTful

注解实现

| HTTP 动词 | Srping Boot 注解 | 说明                 |
| --------- | ---------------- | -------------------- |
| GET       | @GetMapping      | 读取                 |
| POST      | @PostMapping     | 新建                 |
| PUT       | @PutMapping      | 更新                 |
| PATCH     | @PatchMapping    | 更新，通常是部分更新 |
| DELETE    | @DeleteMapping   | 删除                 |

**实践 RESTful**

统一返回对象

```java
/**
 * 统一返回对象
 *
 * @param <T>
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 调用成功（无数据返回）
     */
    public static ApiResponse ok() {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse(success.getCode(), success.getMessage(), null);
    }

    /**
     * 调用成功（数据返回）
     */
    public static ApiResponse ok(Object data) {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse(success.getCode(), success.getMessage(), data);
    }

    /**
     * 调用失败
     */
    public static ApiResponse failure(ResultStatus status) {
        return new ApiResponse(status.getCode(), status.getMessage(), null);
    }

    /**
     * 调用失败（附带详细描述）
     */
    public static ApiResponse failure(ResultStatus status, Object data) {
        return new ApiResponse(status.getCode(), status.getMessage(), data);
    }

}
```

controller 实现

```java
/**
 * 订单 RESTful 接口
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    /**
     * 查询订单列表
     *
     * @return
     */
    @GetMapping("list")
    public ApiResponse<Order> queryOrderList() {
        return ApiResponse.ok(List.of(Order.builder().build()));
    }

    /**
     * 获取订单详情
     *
     * @param id 订单id
     * @return
     */
    @GetMapping("{id}")
    public ApiResponse<Order> getOrderDetails(@PathVariable Integer id) {
        return ApiResponse.ok(Order.builder().build());
    }

    /**
     * 创建订单
     *
     * @param order 订单信息
     * @return
     */
    @PostMapping
    public ApiResponse createOrder(Order order) {
        // order data persistence
        return ApiResponse.ok();
    }

    /**
     * 更新订单
     *
     * @param order 订单信息
     * @return
     */
    @PutMapping
    public ApiResponse updateOrder(Order order) {
        // order data update
        return ApiResponse.ok();
    }

    /**
     * 删除订单
     *
     * @param id 订单id
     * @return
     */
    @DeleteMapping("{id}")
    public ApiResponse deleteOrder(@PathVariable Integer id) {
        // order data delete
        return ApiResponse.ok();
    }

}
```



清单1：忽略 JSON 返回某字段

```java
@JsonIgnore
private Boolean deleted;
```

