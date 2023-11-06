# springboot-rest-template

Spring Boot RestTemplate HTTP 请求

> RestTemplate 是从 Spring3.0 开始支持的一个 HTTP 请求工具，它提供了常见的REST请求方案的模版，例如 GET 请求、POST 请求、PUT 请求、DELETE 请求以及一些通用的请求执行方法 exchange 以及 execute。

实战讲解

1）web依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

2）配置 Spring 管理

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(15000);
        return factory;
    }

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

}
```

3）HTTP 工具类

```java
// 通用 HTTP 请求
public <R, T> T request(HttpMethod httpMethod, String url, Map<String, String> headerParam, R requestBody, T responseType) {
    HttpHeaders headers = new HttpHeaders();
    headerParam.forEach(headers::add);
    RequestEntity<R> requestEntity = new RequestEntity<R>(requestBody, headers, httpMethod, URI.create(url));
    ResponseEntity<ApiResponse<T>> exchange = restTemplate.exchange(requestEntity, getReference((Class<T>) responseType.getClass()));
    return Objects.requireNonNull(exchange.getBody()).getData();
}
// 返回对象类型声明
private <T> ParameterizedTypeReference<ApiResponse<T>> getReference(Class<T> clazz) {
    return ParameterizedTypeReference.forType(new ParameterizedTypeImpl(new Type[] {clazz}, null, ApiResponse.class));
}
```

:candy:**PS：sun 包下的 ParameterizedTypeImpl 类无法使用，需要添加 fastjson 依赖使用同名类代替使用**

4）上传文件

方法一：借助 commons-io 工具方法 FileUtils.copyInputStreamToFile 实现对文件的复制，然后放入 FileSystemResource

```java
public void request(HttpMethod httpMethod, String url, Map<String, String> extraParam, InputStream is, String fileName) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    extraParam.forEach(headers::add);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>(1);
    File file = new File(Objects.requireNonNull(fileName));
    FileUtils.copyInputStreamToFile(is, file);
    FileSystemResource resource = new FileSystemResource(file);
    form.add("file", resource);
    RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(form, headers, httpMethod, URI.create(url));
    ResponseEntity<ApiResponse> exchange = restTemplate.exchange(requestEntity, ApiResponse.class);
    if (ResultStatus.SUCCESS.getCode() != Objects.requireNonNull(exchange.getBody()).getCode()) {
        throw new RuntimeException("上传文件异常:".concat(exchange.getBody().getMessage()));
    }
}
```

方法二：拜托 FileSystemResource，使用自定义类 InMemoryResource 继承自 ByteArrayResource

InMemoryResource .class

```java
public class InMemoryResource extends ByteArrayResource {

    private String filename;
    private long lastModified;

    public InMemoryResource(byte[] byteArray, String description, String filename, long lastModified) {
        super(byteArray, description);
        this.filename = filename;
        this.lastModified = lastModified;
    }

    @Override
    public long lastModified() throws IOException {
        return lastModified;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
```

HTTPUtil.class

```java
public void request2(HttpMethod httpMethod, String url, Map<String, String> extraParam, InputStream is, String filename) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    extraParam.forEach(headers::add);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>(1);   //需要MultiValueMap数据结构
    InMemoryResource imr = new InMemoryResource(is.readAllBytes(), "", filename, System.currentTimeMillis());
    form.add("file", imr);
    RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(form, headers, httpMethod, URI.create(url));
    ResponseEntity<ApiResponse> exchange = restTemplate.exchange(requestEntity, ApiResponse.class);
    if (ResultStatus.SUCCESS.getCode() != Objects.requireNonNull(exchange.getBody()).getCode()) {
        throw new RuntimeException("上传文件异常:".concat(exchange.getBody().getMessage()));
    }
}
```

