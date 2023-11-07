# springboot-minio

Spring Boot Minio 对象存储

**Minio**

一个开源的分布式对象存储系统，类似各大厂商的 OSS 对象存储服务，可以为企业提供文件数据存储。

**Docker 部署**

```shell
docker run -d --name minio -p 9000:9000 -p 9001:9001 -e "MINIO_ROOT_USER=admin" -e "MINIO_ROOT_PASSWORD=admin123" minio/minio server --console-address ":9000" --address ":9001" /data
```

**功能集成**

1）添加依赖

```xml
<!--  minio 依赖  -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.3.9</version>
</dependency>
<!--  minio 依赖于 okhttp  -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.9.0</version>
</dependency>
```

2）自定义配置类

```java
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioPropertiesConfig {

    private String endpoint;
    private String bucketName;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

}
```

3）配置参数

```yaml
minio:
  endpoint: http://localhost:9001
  bucket-name: bsp
  accessKey: admin
  secretKey: admin123
```

4）工具类

```java
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioPropertiesConfig config;
    private final MinioClient minioClient;

    @PostConstruct
    public void init() {
        boolean exists = bucketExists(config.getBucketName());
        if (!exists) {
            makeBucket(config.getBucketName());
        }
    }

    @SneakyThrows
    public boolean bucketExists(String bucketName) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    @SneakyThrows
    public void makeBucket(String bucketName) {
        boolean exists = bucketExists(bucketName);
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @SneakyThrows
    public void removeBucket(String bucketName) {
        boolean exists = bucketExists(bucketName);
        if (exists) {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @SneakyThrows
    public List<Bucket> listBucket() {
        return minioClient.listBuckets();
    }

    @SneakyThrows
    public void upload(String bucketName, MultipartFile file, String filename) {
        upload(bucketName, new ByteArrayInputStream(file.getBytes()), filename, file.getContentType());
    }

    @SneakyThrows
    public void upload(String bucketName, InputStream file, String filename, String contentType) {
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(filename)
                .stream(file, file.available(), -1).contentType(contentType).build());
    }

    @SneakyThrows
    public String getUrl(String bucketName, String filename) {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(filename)
                .expiry(2, TimeUnit.MINUTES)
                .build());
    }

    @SneakyThrows
    public void remove(String bucketName, String filename) {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
    }

    @SneakyThrows
    public InputStream get(String bucketName, String filename) {
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(filename).build());
    }

}
```

5）文件上传

![](D:\workspace\practice-master\springboot-master\code\springboot-minio\Snipaste_2023-10-12_16-32-34.png)

**添加文件元数据持久化**

1）建立元数据对象

```java
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FileMetadata {

    @Id
    @GenericGenerator(name = "jpa-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "jpa-uuid")
    private String id;
    private String name;
    private String suffix;
    private String mime;
    @Enumerated(EnumType.STRING)
    private StorageType storageType; // MINIO
    @CreatedDate
    private LocalDateTime createdDate;

}
```

2）解析文件 MIME 类型

借助 Apache Tika 检测文件类型

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.8.0</version>
</dependency>
```

示例：

```java
String mime = new Tika().detect(new FileInputStream(""))
```

