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

}
