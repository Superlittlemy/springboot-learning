package com.slm.minio.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    void upload(MultipartFile file, String filename);

    void remove(String filename);

    String getUrl(String filename);

    InputStream getInputStream(String filename);

}
