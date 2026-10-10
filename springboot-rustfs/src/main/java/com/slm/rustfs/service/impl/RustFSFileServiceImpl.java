package com.slm.rustfs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.rustfs.config.RustFSProperties;
import com.slm.rustfs.convert.FileConvert;
import com.slm.rustfs.entity.FileMetadata;
import com.slm.rustfs.enums.ResultStatus;
import com.slm.rustfs.exception.FileException;
import com.slm.rustfs.mapper.FileMetadataMapper;
import com.slm.rustfs.model.AccessUrlResponse;
import com.slm.rustfs.model.FileResponse;
import com.slm.rustfs.service.FileService;
import com.slm.rustfs.utils.RustFSUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RustFSFileServiceImpl extends ServiceImpl<FileMetadataMapper, FileMetadata> implements FileService {

    /**
     * 最大上传文件大小
     */
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    /**
     * 预签名有效期下限(秒)
     */
    private static final int MIN_EXPIRY_SECONDS = 60;

    /**
     * 预签名有效期上限(秒):预签名URL硬限制7天
     */
    private static final int MAX_EXPIRY_SECONDS = 7 * 24 * 3600;

    /**
     * 预签名有效期缺省值(秒):配置未提供时的兜底
     */
    private static final int DEFAULT_EXPIRY_SECONDS = 1800;
    
    /**
     * 文件日期路径
     */
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private RustFSProperties rustFSProperties;

    @Autowired 
    private RustFSUtil rustFSUtil;

    @Autowired 
    private FileConvert fileConvert;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(ResultStatus.BAD_REQUEST, "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new FileException(ResultStatus.FILE_SIZE_EXCEEDED, "文件大小不能超过 20MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new FileException(ResultStatus.BAD_REQUEST, "读取文件失败: " + e.getMessage());
        }
        String md5 = DigestUtils.md5DigestAsHex(bytes);

        FileMetadata existing = this.lambdaQuery()
                .eq(FileMetadata::getMd5, md5)
                .one();
        if (existing != null) {
            return fileConvert.toResponse(existing);
        }

        this.getBaseMapper().physicalDeleteSoftDeletedByMd5(md5);

        String ext = extractExtension(file.getOriginalFilename());
        String objectKey = LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID() + ext;

        FileMetadata entity = new FileMetadata();
        entity.setOriginalName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setMd5(md5);
        entity.setObjectKey(objectKey);

        try {
            this.save(entity);
            rustFSUtil.putObject(file, objectKey);
            return fileConvert.toResponse(entity);
        } catch (DuplicateKeyException dup) {
            FileMetadata winner = this.lambdaQuery()
                    .eq(FileMetadata::getMd5, md5)
                    .one();
            return fileConvert.toResponse(winner);
        }
    }

    @Override
    public List<AccessUrlResponse> getAccessUrls(List<String> objectKeys, Integer expiresSeconds) {
        int expiry = resolveExpiry(expiresSeconds);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiry);

        List<String> distinctKeys = objectKeys.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        Map<String, FileMetadata> existing = new HashMap<>();
        if (!distinctKeys.isEmpty()) {
            this.lambdaQuery()
                    .in(FileMetadata::getObjectKey, distinctKeys)
                    .list()
                    .forEach(e -> existing.put(e.getObjectKey(), e));
        }

        List<AccessUrlResponse> result = new ArrayList<>();
        for (String key : distinctKeys) {
            if (!existing.containsKey(key)) {
                continue;
            }
            FileMetadata fileMetadata = existing.get(key);
            AccessUrlResponse r = new AccessUrlResponse();
            r.setObjectKey(key);
            r.setOriginalName(fileMetadata.getOriginalName());
            r.setAccessUrl(rustFSUtil.getPresignedUrl(key, expiry));
            r.setExpiresAt(expiresAt);
            result.add(r);
        }
        return result;
    }

    private int resolveExpiry(Integer expiresSeconds) {
        Integer configured = rustFSProperties.getPresignExpirationSeconds();
        int expiry = expiresSeconds != null ? expiresSeconds
                : (configured != null ? configured : DEFAULT_EXPIRY_SECONDS);
        return Math.max(MIN_EXPIRY_SECONDS, Math.min(expiry, MAX_EXPIRY_SECONDS));
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 || dot == filename.length() - 1 ? "" : filename.substring(dot);
    }

}
