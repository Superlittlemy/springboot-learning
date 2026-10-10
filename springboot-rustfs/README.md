# springboot-rustfs

> Spring Boot 集成 RustFS 对象存储

**RustFS 简介**

RustFS 是使用目前全球最流行的、内存安全的 Rust 语言开发的 高性能 ， 分布式对象存储软件。RustFS 同时具备简单、高效的特点，也是一种高效、开源、自由、可以进行本地私有云部署的对象存储解决方案。RustFS 100% 兼容 S3 协议，使用 Apache2 许可证发行的开源软件。RustFS 使用目前全世界最受欢迎的、内存安全的语言 Rust 语言编写。 它是由全世界优秀的工程师参与并贡献的一款对于商用友好的分布式对象存储产品，RustFS 可以平替非常多非友好开源协议的对象存储产品。

**Docker Compose 安装**

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

**Spring Boot 集成**

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
