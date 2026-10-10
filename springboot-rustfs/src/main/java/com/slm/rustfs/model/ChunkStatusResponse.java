package com.slm.rustfs.model;

import java.util.List;

import lombok.Data;

/**
 * 分片上传进度响应(断点续传依据)
 */
@Data
public class ChunkStatusResponse {

    /**
     * 分片上传ID
     */
    private String uploadId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件总大小(字节)
     */
    private Long totalSize;

    /**
     * 分片大小(字节)
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

    /**
     * 已上传的分片序号(升序)
     */
    private List<Integer> uploadedPartNumbers;

}