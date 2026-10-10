package com.slm.rustfs.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.slm.rustfs.config.RustFSProperties;
import com.slm.rustfs.enums.ResultStatus;
import com.slm.rustfs.exception.FileException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j 
@Component 
public class RustFSUtil {
    
    @Autowired 
    private S3Client client;

    @Autowired 
    private S3Presigner presigner;

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

    
    /**
     * 生成带过期时间的GET预签名URL(私有bucket文件访问的唯一入口)
     */
    public String getPresignedUrl(String objectKey, int expirySeconds) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(GetObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build())
                .signatureDuration(Duration.ofSeconds(expirySeconds)) // 有效期
                .build();
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("生成预签名URL失败: objectKey={}", objectKey, e);
            throw new FileException(ResultStatus.MINIO_OPERATION_ERROR, "生成文件访问地址失败: " + e.getMessage());
        }
    }

    /**
     * 删除对象
     */
    public void removeObject(String objectKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build());
        } catch (Exception e) {
            log.warn("删除对象失败: objectKey={}", objectKey, e);
        }
    }

    /**
     * 发起分片上传
     *
     * @return 对象存储返回的 uploadId
     */
    public String createMultipartUpload(String objectKey, String contentType) {
        try {
            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
            return client.createMultipartUpload(request).uploadId();
        } catch (Exception e) {
            log.error("发起分片上传失败: objectKey={}", objectKey, e);
            throw new FileException(ResultStatus.MINIO_OPERATION_ERROR, "发起分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传单个分片
     *
     * @return 分片ETag,合并时需原样回传
     */
    public String uploadPart(String objectKey, String uploadId, int partNumber, byte[] content) {
        try {
            UploadPartRequest request = UploadPartRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();
            return client.uploadPart(request, RequestBody.fromBytes(content)).eTag();
        } catch (Exception e) {
            log.error("上传分片失败: objectKey={}, uploadId={}, partNumber={}", objectKey, uploadId, partNumber, e);
            throw new FileException(ResultStatus.MINIO_OPERATION_ERROR, "上传分片失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片(parts必须按分片序号升序)
     */
    public void completeMultipartUpload(String objectKey, String uploadId, List<CompletedPart> parts) {
        try {
            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                .build();
            client.completeMultipartUpload(request);
        } catch (Exception e) {
            log.error("合并分片失败: objectKey={}, uploadId={}", objectKey, uploadId, e);
            throw new FileException(ResultStatus.MINIO_OPERATION_ERROR, "合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 读取对象内容并计算整文件 MD5
     * <p>
     * 分片上传的各片是独立请求到达的,MD5 无法在多次请求之间增量延续
     * (摘要中间状态不可序列化),因此合并后从对象存储流式回读一次来计算内容摘要。
     * 读取发生在服务端与对象存储之间,不会占用外部带宽。
     */
    public String calculateMd5(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(properties.getBucket())
            .key(objectKey)
            .build();
        try (ResponseInputStream<GetObjectResponse> stream = client.getObject(request)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            // 注意:这里是已完成计算的摘要结果,只能转十六进制,不能再走 DigestUtils.md5DigestAsHex(会二次哈希)
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new FileException(ResultStatus.ERROR, "当前运行环境不支持 MD5");
        } catch (Exception e) {
            log.error("计算文件MD5失败: objectKey={}", objectKey, e);
            throw new FileException(ResultStatus.MINIO_OPERATION_ERROR, "计算文件MD5失败: " + e.getMessage());
        }
    }

    /**
     * 字节数组转小写十六进制
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * 取消分片上传,释放对象存储侧已上传但未合并的分片
     */
    public void abortMultipartUpload(String objectKey, String uploadId) {
        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .uploadId(uploadId)
                .build();
            client.abortMultipartUpload(request);
        } catch (Exception e) {
            log.warn("取消分片上传失败: objectKey={}, uploadId={}", objectKey, uploadId, e);
        }
    }

}
