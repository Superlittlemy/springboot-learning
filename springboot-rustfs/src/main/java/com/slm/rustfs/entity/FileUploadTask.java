package com.slm.rustfs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分片上传任务
 * <p>
 * 一次分片上传对应对象存储侧的一个 multipart upload,任务在合并完成后(或取消后)被清理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_upload_task")
public class FileUploadTask extends BaseEntity {

    /**
     * 对象存储返回的分片上传ID
     */
    @TableField("upload_id")
    private String uploadId;

    /**
     * 目标对象Key(初始化时生成,合并前不真正落库为文件元数据)
     */
    @TableField("object_key")
    private String objectKey;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * MIME类型
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 文件总大小(字节)
     */
    @TableField("total_size")
    private Long totalSize;

    /**
     * 分片大小(字节)
     */
    @TableField("chunk_size")
    private Integer chunkSize;

    /**
     * 分片总数
     */
    @TableField("total_chunks")
    private Integer totalChunks;

}