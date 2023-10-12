package com.slm.minio.utils;

import com.slm.minio.config.MinioPropertiesConfig;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
