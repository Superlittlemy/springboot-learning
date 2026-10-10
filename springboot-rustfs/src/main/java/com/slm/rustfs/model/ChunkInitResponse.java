package com.slm.rustfs.model;

import lombok.Data;

/**
 * 分片上传初始化响应
 */
@Data
public class ChunkInitResponse {

    /**
     * 分片上传ID,后续上传分片/查询进度/合并/取消均以此为凭据
     */
    private String uploadId;

    /**
     * 目标对象Key
     */
    private String objectKey;

    /**
     * 生效的分片大小(字节)
     */
    private Integer chunkSize;

    /**
     * 分片总数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

}