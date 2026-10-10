package com.slm.rustfs.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.rustfs.convert.FileConvert;
import com.slm.rustfs.entity.FileMetadata;
import com.slm.rustfs.entity.FileUploadPart;
import com.slm.rustfs.entity.FileUploadTask;
import com.slm.rustfs.enums.ResultStatus;
import com.slm.rustfs.exception.FileException;
import com.slm.rustfs.mapper.FileMetadataMapper;
import com.slm.rustfs.mapper.FileUploadPartMapper;
import com.slm.rustfs.mapper.FileUploadTaskMapper;
import com.slm.rustfs.model.ChunkCompleteResponse;
import com.slm.rustfs.model.ChunkInitRequest;
import com.slm.rustfs.model.ChunkInitResponse;
import com.slm.rustfs.model.ChunkStatusResponse;
import com.slm.rustfs.model.ChunkUploadResponse;
import com.slm.rustfs.model.InstantUploadRequest;
import com.slm.rustfs.model.InstantUploadResponse;
import com.slm.rustfs.service.ChunkUploadService;
import com.slm.rustfs.service.FileService;
import com.slm.rustfs.utils.RustFSUtil;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.CompletedPart;

@Slf4j
@Service
public class RustFSChunkUploadServiceImpl extends ServiceImpl<FileUploadTaskMapper, FileUploadTask> implements ChunkUploadService {

    /**
     * 缺省分片大小:5MB,兼顾 S3 分片下限与单请求体积
     */
    public static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;

    /**
     * 最小分片大小:除最后一片外,S3 要求分片不小于 5MB
     */
    private static final int MIN_CHUNK_SIZE = 5 * 1024 * 1024;

    /**
     * 最大分片大小:与 spring.servlet.multipart.max-file-size 保持一致
     */
    private static final int MAX_CHUNK_SIZE = 20 * 1024 * 1024;

    /**
     * 分片总数上限:S3 单次分片上传上限 10000 片
     */
    private static final int MAX_PART_COUNT = 10000;

    /**
     * 分片上传文件总大小上限:5GB
     */
    private static final long MAX_TOTAL_SIZE = 5L * 1024 * 1024 * 1024;

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private RustFSUtil rustFSUtil;

    @Autowired
    private FileConvert fileConvert;

    @Autowired
    private FileUploadPartMapper fileUploadPartMapper;

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Autowired
    private FileService fileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkInitResponse init(ChunkInitRequest request) {
        long fileSize = request.getFileSize();
        if (fileSize > MAX_TOTAL_SIZE) {
            throw new FileException(ResultStatus.FILE_SIZE_EXCEEDED, "分片上传支持的最大文件为 5GB");
        }

        int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : DEFAULT_CHUNK_SIZE;
        if (chunkSize < MIN_CHUNK_SIZE || chunkSize > MAX_CHUNK_SIZE) {
            throw new FileException(ResultStatus.CHUNK_SIZE_INVALID, "分片大小需在 5MB ~ 20MB 之间");
        }

        int totalChunks = (int) ((fileSize + chunkSize - 1) / chunkSize);
        if (totalChunks > MAX_PART_COUNT) {
            throw new FileException(ResultStatus.FILE_SIZE_EXCEEDED, "分片数量不能超过 10000,请增大分片大小");
        }

        String contentType = StringUtils.hasText(request.getContentType())
            ? request.getContentType() : DEFAULT_CONTENT_TYPE;

        // 客户端已算出 MD5 时先判断能否闪传;命中则不创建 multipart upload、不登记上传任务
        if (StringUtils.hasText(request.getMd5())) {
            InstantUploadRequest probe = new InstantUploadRequest();
            probe.setMd5(request.getMd5());
            probe.setFileSize(fileSize);
            probe.setFileName(request.getFileName());
            InstantUploadResponse instant = fileService.instantUpload(probe);
            if (Boolean.TRUE.equals(instant.getInstant())) {
                log.info("分片初始化命中闪传: md5={}, fileName={}", request.getMd5(), request.getFileName());
                ChunkInitResponse hit = new ChunkInitResponse();
                hit.setInstant(true);
                hit.setFile(instant.getFile());
                return hit;
            }
        }

        String objectKey = LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID() + extractExtension(request.getFileName());

        String uploadId = rustFSUtil.createMultipartUpload(objectKey, contentType);

        FileUploadTask task = new FileUploadTask();
        task.setUploadId(uploadId);
        task.setObjectKey(objectKey);
        task.setOriginalName(request.getFileName());
        task.setContentType(contentType);
        task.setTotalSize(fileSize);
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        try {
            this.save(task);
        } catch (RuntimeException e) {
            // 任务登记失败时必须回收对象存储侧的分片上传,否则分片会一直占空间
            rustFSUtil.abortMultipartUpload(objectKey, uploadId);
            throw e;
        }

        ChunkInitResponse response = new ChunkInitResponse();
        response.setInstant(false);
        response.setUploadId(uploadId);
        response.setObjectKey(objectKey);
        response.setChunkSize(chunkSize);
        response.setTotalChunks(totalChunks);
        response.setUploadedChunks(0);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadResponse uploadPart(String uploadId, int partNumber, MultipartFile file) {
        FileUploadTask task = requireTask(uploadId);

        if (file == null || file.isEmpty()) {
            throw new FileException(ResultStatus.BAD_REQUEST, "分片内容为空");
        }
        if (file.getSize() > task.getChunkSize()) {
            throw new FileException(ResultStatus.CHUNK_SIZE_INVALID,
                "分片大小超过约定的 " + task.getChunkSize() + " 字节");
        }
        if (partNumber < 1 || partNumber > task.getTotalChunks()) {
            throw new FileException(ResultStatus.BAD_REQUEST,
                "分片序号需在 1 ~ " + task.getTotalChunks() + " 之间");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            log.error("读取分片失败: uploadId={}, partNumber={}", uploadId, partNumber, e);
            throw new FileException(ResultStatus.BAD_REQUEST, "读取分片失败: " + e.getMessage());
        }

        // 对象存储侧同序号分片会被覆盖,因此重传天然幂等
        String eTag = rustFSUtil.uploadPart(task.getObjectKey(), uploadId, partNumber, content);

        FileUploadPart part = new FileUploadPart();
        part.setUploadId(uploadId);
        part.setPartNumber(partNumber);
        part.setETag(eTag);
        part.setSizeBytes(file.getSize());
        upsertPart(part);

        ChunkUploadResponse response = new ChunkUploadResponse();
        response.setPartNumber(partNumber);
        response.setETag(eTag);
        response.setSizeBytes(file.getSize());
        response.setUploadedChunks(countParts(uploadId));
        response.setTotalChunks(task.getTotalChunks());
        return response;
    }

    @Override
    public ChunkStatusResponse status(String uploadId) {
        FileUploadTask task = requireTask(uploadId);
        List<FileUploadPart> parts = listParts(uploadId);

        ChunkStatusResponse response = new ChunkStatusResponse();
        response.setUploadId(uploadId);
        response.setOriginalName(task.getOriginalName());
        response.setTotalSize(task.getTotalSize());
        response.setChunkSize(task.getChunkSize());
        response.setTotalChunks(task.getTotalChunks());
        response.setUploadedChunks(parts.size());
        response.setUploadedPartNumbers(parts.stream()
            .map(FileUploadPart::getPartNumber)
            .collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkCompleteResponse complete(String uploadId) {
        FileUploadTask task = requireTask(uploadId);
        List<FileUploadPart> parts = listParts(uploadId);

        if (parts.size() != task.getTotalChunks()) {
            throw new FileException(ResultStatus.BAD_REQUEST,
                "分片未上传完整,已上传 " + parts.size() + "/" + task.getTotalChunks() + " 片");
        }
        long uploadedSize = parts.stream().mapToLong(FileUploadPart::getSizeBytes).sum();
        if (uploadedSize != task.getTotalSize()) {
            throw new FileException(ResultStatus.BAD_REQUEST,
                "分片总大小与声明的文件大小不一致(" + uploadedSize + " != " + task.getTotalSize() + ")");
        }

        List<CompletedPart> completedParts = parts.stream()
            .map(p -> CompletedPart.builder().partNumber(p.getPartNumber()).eTag(p.getETag()).build())
            .collect(Collectors.toList());
        rustFSUtil.completeMultipartUpload(task.getObjectKey(), uploadId, completedParts);

        String md5 = rustFSUtil.calculateMd5(task.getObjectKey());

        // 与普通上传共用同一套内容去重:命中则丢弃本次合并出的对象,复用历史记录
        FileMetadata existing = fileMetadataMapper.selectOne(
            new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getMd5, md5));
        if (existing != null) {
            rustFSUtil.removeObject(task.getObjectKey());
            cleanup(uploadId);
            return toCompleteResponse(existing, true);
        }

        fileMetadataMapper.physicalDeleteSoftDeletedByMd5(md5);

        FileMetadata entity = new FileMetadata();
        entity.setOriginalName(task.getOriginalName());
        entity.setContentType(task.getContentType());
        entity.setSizeBytes(task.getTotalSize());
        entity.setMd5(md5);
        entity.setObjectKey(task.getObjectKey());
        try {
            fileMetadataMapper.insert(entity);
        } catch (DuplicateKeyException dup) {
            // 并发上传同一文件时以先落库的一方为准
            FileMetadata winner = fileMetadataMapper.selectOne(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getMd5, md5));
            rustFSUtil.removeObject(task.getObjectKey());
            cleanup(uploadId);
            return toCompleteResponse(winner, true);
        }

        cleanup(uploadId);
        return toCompleteResponse(entity, false);
    }

    @Override
    public void abort(String uploadId) {
        FileUploadTask task = requireTask(uploadId);
        rustFSUtil.abortMultipartUpload(task.getObjectKey(), uploadId);
        cleanup(uploadId);
    }

    private FileUploadTask requireTask(String uploadId) {
        if (!StringUtils.hasText(uploadId)) {
            throw new FileException(ResultStatus.BAD_REQUEST, "uploadId 不能为空");
        }
        FileUploadTask task = this.lambdaQuery().eq(FileUploadTask::getUploadId, uploadId).one();
        if (task == null) {
            throw new FileException(ResultStatus.UPLOAD_TASK_NOT_FOUND, "上传任务不存在或已完成:" + uploadId);
        }
        return task;
    }

    /**
     * 分片重传时覆盖原记录,保证 (upload_id, part_number) 唯一
     */
    private void upsertPart(FileUploadPart part) {
        FileUploadPart existing = fileUploadPartMapper.selectOne(new LambdaQueryWrapper<FileUploadPart>()
            .eq(FileUploadPart::getUploadId, part.getUploadId())
            .eq(FileUploadPart::getPartNumber, part.getPartNumber()));
        if (existing == null) {
            fileUploadPartMapper.insert(part);
            return;
        }
        part.setId(existing.getId());
        fileUploadPartMapper.updateById(part);
    }

    private int countParts(String uploadId) {
        return fileUploadPartMapper.selectCount(new LambdaQueryWrapper<FileUploadPart>()
            .eq(FileUploadPart::getUploadId, uploadId)).intValue();
    }

    private List<FileUploadPart> listParts(String uploadId) {
        return fileUploadPartMapper.selectList(new LambdaQueryWrapper<FileUploadPart>()
            .eq(FileUploadPart::getUploadId, uploadId)
            .orderByAsc(FileUploadPart::getPartNumber));
    }

    private void cleanup(String uploadId) {
        fileUploadPartMapper.deleteByUploadId(uploadId);
        this.getBaseMapper().physicalDeleteByUploadId(uploadId);
    }

    private ChunkCompleteResponse toCompleteResponse(FileMetadata metadata, boolean deduplicated) {
        ChunkCompleteResponse response = new ChunkCompleteResponse();
        response.setFile(fileConvert.toResponse(metadata));
        response.setDeduplicated(deduplicated);
        return response;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 || dot == filename.length() - 1 ? "" : filename.substring(dot);
    }

}