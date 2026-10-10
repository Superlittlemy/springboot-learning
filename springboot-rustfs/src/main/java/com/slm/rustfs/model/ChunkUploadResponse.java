package com.slm.rustfs.model;

import lombok.Data;

/**
 * 单个分片上传响应
 */
@Data
public class ChunkUploadResponse {

    /**
     * 分片序号
     */
    private Integer partNumber;

    /**
     * 分片ETag
     */
    private String eTag;

    /**
     * 分片大小(字节)
     */
    private Long sizeBytes;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 分片总数
     */
    private Integer totalChunks;

}