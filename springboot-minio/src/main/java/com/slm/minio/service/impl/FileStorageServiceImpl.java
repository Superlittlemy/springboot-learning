package com.slm.minio.service.impl;

import com.slm.minio.config.MinioPropertiesConfig;
import com.slm.minio.entity.enums.StorageType;
import com.slm.minio.entity.table.FileMetadata;
import com.slm.minio.repository.FileMetadataRepository;
import com.slm.minio.service.FileStorageService;
import com.slm.minio.utils.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioPropertiesConfig config;
    private final MinioUtil minioUtil;
    private final FileMetadataRepository fileMetadataRepository;

    public String getOSSFilename(FileMetadata fileMetadata) {
        return fileMetadata.getId().concat(".").concat(fileMetadata.getSuffix());
    }

    @Override
    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public String upload(MultipartFile file, String filename) {
        String originalName = file.getOriginalFilename();
        int suffixIndex = originalName.lastIndexOf(".");
        String mime = new Tika().detect(file.getInputStream());
        FileMetadata fileMetadata = fileMetadataRepository.save(FileMetadata.builder()
                .name(suffixIndex > 0 ? filename.substring(0, suffixIndex - 1) : filename)
                .suffix(suffixIndex > 0 ? originalName.substring(originalName.lastIndexOf(".")) : originalName)
                .mime(mime)
                .storageType(StorageType.MINIO)
                .build());
        minioUtil.upload(config.getBucketName(), file, getOSSFilename(fileMetadata));
        return fileMetadata.getId();
    }

    @Override
    public void remove(String id) {
        fileMetadataRepository.findById(id).ifPresent(fileMetadata -> {
            minioUtil.remove(config.getBucketName(), getOSSFilename(fileMetadata));
        });
    }

    @Override
    public String getUrl(String id) {
        return fileMetadataRepository.findById(id).map(fileMetadata ->
            minioUtil.getUrl(config.getBucketName(), getOSSFilename(fileMetadata))
        ).orElseThrow(() -> new RuntimeException("文件不存在"));
    }

    @Override
    public InputStream getInputStream(String id) {
        return fileMetadataRepository.findById(id).map(fileMetadata ->
            minioUtil.get(config.getBucketName(), getOSSFilename(fileMetadata))
        ).orElseThrow(() -> new RuntimeException("文件不存在"));
    }

}
