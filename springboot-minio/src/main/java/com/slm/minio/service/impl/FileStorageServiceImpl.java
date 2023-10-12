package com.slm.minio.service.impl;

import com.slm.minio.config.MinioPropertiesConfig;
import com.slm.minio.service.FileStorageService;
import com.slm.minio.utils.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioPropertiesConfig config;
    private final MinioUtil minioUtil;

    @Override
    public void upload(MultipartFile file, String filename) {
        minioUtil.upload(config.getBucketName(), file, filename);
    }

    @Override
    public void remove(String filename) {
        minioUtil.remove(config.getBucketName(), filename);
    }

    @Override
    public String getUrl(String filename) {
        return minioUtil.getUrl(config.getBucketName(), filename);
    }

    @Override
    public InputStream getInputStream(String filename) {
        return minioUtil.get(config.getBucketName(), filename);
    }

}
