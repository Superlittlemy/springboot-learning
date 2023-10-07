# springboot-openapi

Spring Boot OpenAPI 文档集成（基于 springdoc 的 OpenAPI 3.0）



**OpenAPI 3.0.0 是 OpenAPI 规范的第一个正式版本。**

OpenAPI 规范（OAS），是定义一个标准的、与具体编程语言无关的RESTful API的规范。OpenAPI 规范使得人类和计算机都能在“不接触任何程序源代码和文档、不监控网络通信”的情况下理解一个服务的作用。如果您在定义您的 API 时做的很好，那么使用 API 的人就能非常轻松地理解您提供的 API 并与之交互了。

如果您遵循 OpenAPI 规范来定义您的 API，那么您就可以用文档生成工具来展示您的 API，用代码生成工具来自动生成各种编程语言的服务器端和客户端的代码，用自动测试工具进行测试等等。

*Swagger*

Swagger 是一个 api 文档维护组织，后来成为了 Open API 标准的主要定义者，国内绝大部分人还在用过时的 swagger2。

*SpringFox*

SpringFox 是 Spring 社区维护的一个项目（非官方），帮助使用者将 swagger2 集成到 Spring 中。常常用于 Spring 中帮助开发者生成文档，并可以轻松的在 Spring Boot 中使用。

*SpringDoc*

SpringDoc 也是 Spring 社区维护的一个项目（非官方），帮助使用者将 swagger3 集成到 Spring 中。

> swagger3 与 swagger2 并不兼容，故升级需要涉及迁移



**集成 SpringDoc**

pom.xml 依赖

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

springfox 与 springdoc 注解对应关系

| swagger2 | OpenAPI 3 | 注解位置 |
| ---- | ---- | ---- |
| @Api | @Tag(name = “接口类描述”) | Controller 类上 |
| @ApiOperation | @Operation(summary =“接口方法描述”) | Controller 方法上 |
| @ApiParam | @Parameter(description=“参数描述”) | Controller 方法的参数上 |
| @ApiIgnore | @Parameter(hidden = true) 或 @Operation(hidden = true) 或 @Hidden | - |
| @ApiModel | @Schema | DTO类上 |
| @ApiModelProperty | @Schema | DTO属性上 |

访问路径：{host}/swagger-ui.html

![](D:\workspace\practice-master\springboot-master\code\springboot-openapi\Snipaste_2023-10-07_11-36-59.png)
