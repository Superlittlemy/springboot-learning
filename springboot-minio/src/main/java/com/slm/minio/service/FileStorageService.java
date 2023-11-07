package com.slm.minio.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    String upload(MultipartFile file, String filename);

    void remove(String id);

    String getUrl(String id);

    InputStream getInputStream(String id);

}
