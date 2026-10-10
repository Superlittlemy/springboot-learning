package com.slm.rustfs.service;

import org.springframework.web.multipart.MultipartFile;

import com.slm.rustfs.model.ChunkCompleteResponse;
import com.slm.rustfs.model.ChunkInitRequest;
import com.slm.rustfs.model.ChunkInitResponse;
import com.slm.rustfs.model.ChunkStatusResponse;
import com.slm.rustfs.model.ChunkUploadResponse;

/**
 * 分片上传服务
 * <p>
 * 流程:初始化 -> 上传分片(可乱序、可并发) -> 合并。合并后回读对象计算整文件MD5并落库去重。
 */
public interface ChunkUploadService {

    /**
     * 初始化分片上传,返回 uploadId
     */
    ChunkInitResponse init(ChunkInitRequest request);

    /**
     * 上传单个分片(同序号重复上传视为重传,覆盖原分片)
     */
    ChunkUploadResponse uploadPart(String uploadId, int partNumber, MultipartFile file);

    /**
     * 查询上传进度(断点续传)
     */
    ChunkStatusResponse status(String uploadId);

    /**
     * 合并分片并登记文件元数据
     */
    ChunkCompleteResponse complete(String uploadId);

    /**
     * 取消分片上传并清理任务
     */
    void abort(String uploadId);

}