# springboot-rustfs

> Spring Boot 集成 RustFS 对象存储

## RustFS 简介

RustFS 是使用目前全球最流行的、内存安全的 Rust 语言开发的 高性能 ， 分布式对象存储软件。RustFS 同时具备简单、高效的特点，也是一种高效、开源、自由、可以进行本地私有云部署的对象存储解决方案。RustFS 100% 兼容 S3 协议，使用 Apache2 许可证发行的开源软件。RustFS 使用目前全世界最受欢迎的、内存安全的语言 Rust 语言编写。 它是由全世界优秀的工程师参与并贡献的一款对于商用友好的分布式对象存储产品，RustFS 可以平替非常多非友好开源协议的对象存储产品。

## Docker Compose 安装

```yaml
version: "3.8"

services:
  rustfs:
    image: rustfs/rustfs:latest
    container_name: rustfs-server
    security_opt:
      - "no-new-privileges:true"
    ports:
      - "9000:9000"   # S3 API 对外端口
      - "9001:9001"   # 控制台对外端口
    environment:
      # 数据卷（多个路径用逗号分隔）
      # - RUSTFS_VOLUMES=/data/rustfs0
      # API 和控制台监听地址
      - RUSTFS_ADDRESS=0.0.0.0:9000
      - RUSTFS_CONSOLE_ADDRESS=0.0.0.0:9001
      - RUSTFS_CONSOLE_ENABLE=true
      # CORS 设置，控制台与 S3 API 都放开来源
      - RUSTFS_CORS_ALLOWED_ORIGINS=*
      - RUSTFS_CONSOLE_CORS_ALLOWED_ORIGINS=*
      # 访问密钥（生产环境请修改为强密码）
      - RUSTFS_ACCESS_KEY=rustfsadmin
      - RUSTFS_SECRET_KEY=rustfsadmin
      # 日志级别
      - RUSTFS_OBS_LOGGER_LEVEL=info

    # volumes:
      # 存储数据卷（请根据实际情况修改路径）
      # - ./deploy/data/pro:/data
      # 日志目录
      # - ./deploy/logs:/app/logs

    networks:
      - rustfs-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "sh", "-c", "curl -f http://localhost:9000/health && curl -f http://localhost:9001/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

networks:
  rustfs-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

## Spring Boot 集成

1）添加依赖

```xml
<!-- RustFS | AWS SDK 依赖 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>${aws.version}</version>
</dependency>
```

2）配置连接参数

```yaml
rustfs:
  endpoint: http://localhost:9000
  access-key: rustfsadmin
  secret-key: rustfsadmin
  bucket: bucket
  # 预签名访问地址缺省有效期(秒),客户端可用 expiresSeconds 覆盖,上限7天
  presign-expiration-seconds: 1800
```

3）创建 S3 客户端

```java
package com.slm.rustfs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class RustFSConfiguration {

    @Autowired
    private RustFSProperties properties;

    @Bean
    public S3Client client() {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint())) // RustFS 地址
                .region(Region.US_EAST_1) // 可写死，RustFS 不校验 region
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .forcePathStyle(true) // 关键配置！RustFS 需启用 Path-Style
                .build();
    }

}
```

4）RustFS 工具类 / 上传对象

```java
package com.slm.rustfs.utils;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.slm.rustfs.config.RustFSProperties;
import com.slm.rustfs.enums.ResultStatus;
import com.slm.rustfs.exception.FileException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j 
@Component 
public class RustFSUtil {
    
    @Autowired 
    private S3Client client;

    @Autowired 
    private RustFSProperties properties;

    /**
     * 初始化 Bucket
     */
    @PostConstruct 
    public void init() {
        String bucket = properties.getBucket();
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket created: {}", bucket);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            System.out.println("Bucket already exists.");
        }
    }

    /**
     * 上传对象
     */
    public void putObject(MultipartFile file, String objectKey) {
        try {
            client.putObject(
                PutObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).contentType(file.getContentType()).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (Exception e) {
            log.warn("上传对象失败: objectKey={}", objectKey, e);
            throw new FileException(ResultStatus.MINIO_OPERATION_ERROR, "文件存储失败: " + e.getMessage());
        }
    }

}
```

## 分片上传

大文件走服务端代理的分片上传:每个分片一次 HTTP 请求打到 Spring Boot，再由 Spring Boot 转发给 RustFS 的 Multipart Upload 接口，最后合并成完整对象。好处是断点续传、失败重试只针对单片，且不占用服务端磁盘与内存(内存中最多驻留一片)。

### 1. 建表

先执行 [chunk-upload-schema.sql](src/main/resources/db/chunk-upload-schema.sql) 建两张表:

| 表 | 说明 |
| --- | --- |
| `file_upload_task` | 一次分片上传对应一个任务:目标对象 Key、文件大小、分片大小、分片总数 |
| `file_upload_part` | 分片明细:分片序号与对象存储返回的 ETag，合并时原样回传 |

`file_upload_task` 记录合并后(或取消后)会被清理，`file_upload_part` 随任务物理删除。

### 2. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/storage/file/chunk/init` | 初始化，返回 `uploadId`、分片大小、分片总数 |
| POST | `/storage/file/chunk/part` | 上传一片，参数 `uploadId`、`partNumber`，文件字段名 `file` |
| GET | `/storage/file/chunk/{uploadId}` | 查询进度，返回已上传的分片序号(断点续传依据) |
| POST | `/storage/file/chunk/complete?uploadId=` | 合并分片并登记文件元数据 |
| DELETE | `/storage/file/chunk/{uploadId}` | 取消上传，终止对象存储侧的 multipart upload 并释放已上传分片 |

调用示例:

```bash
# 初始化(分片总数由服务端按文件大小与分片大小计算，不信任客户端传值)
curl -X POST http://localhost:8080/storage/file/chunk/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"movie.mp4","fileSize":524288000,"contentType":"video/mp4","chunkSize":10485760}'

# 上传分片(partNumber 从 1 开始，可乱序、可并发)
curl -X POST "http://localhost:8080/storage/file/chunk/part?uploadId=$UPLOAD_ID&partNumber=1" -F "file=@part_01"

# 合并
curl -X POST "http://localhost:8080/storage/file/chunk/complete?uploadId=$UPLOAD_ID"
```

合并成功返回文件信息与去重标记:

```json
{
  "code": 0,
  "message": "调用成功",
  "data": {
    "file": { "id": 2108829885806333954, "originalName": "movie.mp4", "md5": "040b18ad...", "objectKey": "2026/10/10/xxx.mp4" },
    "deduplicated": false
  }
}
```

### 3. 演示页面

项目内置一个零依赖的演示页面，启动后访问 <http://localhost:8080/storage/> 即可:

- 选择或拖拽文件后立即在浏览器本地流式计算整文件 MD5，并自动发起闪传探测
- 探测命中时直接结束，一个字节都不传;未命中才进入分片上传，可设置分片大小与并发数并实时看到每一片的进度
- 支持暂停 / 继续 / 取消(取消会调用 `DELETE` 真正释放对象存储侧的分片)
- 支持断点续传:粘贴 `uploadId` 点"查询进度"即可接着传剩下的分片
- 上传完成后展示 MD5、对象 Key 与结果类型(闪传 / 命中去重 / 新建)，并可一键换预签名地址预览文件

### 4. 几个设计取舍

**MD5 在合并后回读计算**。普通上传(`/file/upload`)是整块进内存算 MD5;分片上传的各片是彼此独立的 HTTP 请求，
MD5 的中间状态无法在请求之间延续(摘要状态不可序列化)，所以合并完成后从对象存储流式回读一次对象计算 MD5。
读取发生在服务端与对象存储之间，不占用外部带宽;代价是合并时多一次顺序读。
客户端若已自行算出 MD5，可以走[闪传](#闪传)在传输前短路，不必等传完才发现内容重复。

**分片可乱序、可并发**。分片之间没有依赖关系，同序号的分片重复上传视为重传(对象存储侧同序号分片会被覆盖)，因此网络重试是安全的。

**分片大小限制 5MB ~ 20MB**。下限来自 S3 的硬性要求(除最后一片外分片不得小于 5MB)，
上限与 `spring.servlet.multipart.max-file-size` 对齐，分片总数上限 10000，文件总大小上限 5GB。

**内容去重与普通上传共用一套**。合并后按 MD5 查 `file_metadata`，命中则删掉本次合并出的对象，直接复用历史记录，
响应里 `deduplicated` 为 `true`;未命中则插入新元数据。这样两条上传链路产出的记录对 `/file/access-url` 完全等价。

**任务与分片明细会被清理**。合并成功或取消后，任务与分片记录一并删除。生产环境还需要补一个定时任务，
扫描超过一定时间(比如 24 小时)仍未合并的 `file_upload_task`，调用 `abort` 接口回收对象存储侧残留的分片，
否则客户端异常退出后分片会一直占用存储空间。

## 闪传

分片链路上的去重发生在**合并之后**:文件已经完整传完、服务端也已经合并出了对象，才发现内容重复，此时那次传输和那个对象都白费了。闪传把 MD5 判断**前移到传输之前**——客户端先算出整文件 MD5，服务端查 `file_metadata`，命中就直接返回历史记录，一个字节都不传。

客户端算 MD5 的前提:浏览器的 SubtleCrypto 不支持 MD5，且大文件无法整块读进内存一次性计算，所以演示页内置了一份纯 JS 增量实现 [static/js/md5.js](src/main/resources/static/js/md5.js)，按 4MB 一段流式喂入摘要对象，不需要引入 spark-md5 之类的外部库(内网环境 CDN 往往不可达)。实测吞吐在几十 MB/s 量级，GB 级文件的本地计算耗时会有明显感，因此计算过程带进度显示。

### 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/storage/file/instant-upload` | 闪传探测，请求体 `{md5, fileSize, fileName}`;命中返回 `instant=true` 与历史文件信息 |
| POST | `/storage/file/chunk/init` | 请求体携带 `md5` 时，命中则不创建 multipart upload、不登记上传任务，直接返回 `instant=true` |
| POST | `/storage/file/upload` | 请求头 `X-File-Md5` 命中时直接返回，不落库也不写对象存储 |

三个入口共用 `FileService#instantUpload` 一个方法，判断逻辑只有一份。

```bash
# 探测:命中返回既有文件，前端直接拿 objectKey 去换预签名地址即可
curl -X POST http://localhost:8080/storage/file/instant-upload \
  -H "Content-Type: application/json" \
  -d '{"md5":"040b18ad...","fileSize":524288000,"fileName":"movie.mp4"}'

# 命中
{"code":0,"message":"调用成功","data":{
  "instant":true,
  "file":{"id":2108829885806333954,"originalName":"movie.mp4","md5":"040b18ad...","objectKey":"2026/10/10/xxx.mp4"}
}}

# 未命中(客户端继续走分片上传)
{"code":0,"message":"调用成功","data":{"instant":false,"file":null}}
```

`/file/chunk/init` 携带 `md5` 时的响应:

```json
{
  "code": 0,
  "message": "调用成功",
  "data": {
    "instant": true,
    "file": { "id": 2108829885806333954, "originalName": "movie.mp4", "md5": "040b18ad...", "objectKey": "2026/10/10/xxx.mp4" },
    "uploadId": null, "objectKey": null, "chunkSize": null, "totalChunks": null, "uploadedChunks": null
  }
}
```

客户端凭 `instant` 字段分流:为 `true` 就直接使用 `file` 结束上传，不再发起任何分片请求。

### 客户端上报的 MD5 不可信

浏览器算出来的 MD5 是客户端输入，服务端必须设三道防线:

**格式白名单 + 统一转小写**。只接受 32 位十六进制，非法直接报 `FILE_MD5_INVALID`(3006)。
大小写必须归一——不同客户端产出的大小写不一致，不转就会出现"明明存在却漏命中"。

**文件大小双重校验**。请求里带上 `fileSize`，与历史记录的 `size_bytes` 比对，不一致按未命中处理。
前端 MD5 算错且碰巧撞上别人的记录时，这一步会挡住，不会把别的文件返回给调用方。

**客户端 MD5 绝不落库**。这是最关键的一条:上报的 MD5 只用于"要不要传"的瞬时判断，
`file_upload_task` 不存它，`file_metadata.md5` 里的每一个值都来自服务端对真实对象字节的回读。
因此 `complete` 阶段始终要回读一次对象重算 MD5，不能为了省这一次读就把客户端的值写进表里——
否则任何人都能把任意内容的 MD5 标记成某个已知 MD5，顶替掉那条记录。
代价是每次合并多一次顺序读，但这次读发生在服务端与 RustFS 之间，不占外部带宽。



