package com.slm.rustfs.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

/**
 * 分片上传初始化请求
 */
@Data
public class ChunkInitRequest {

    /**
     * 原始文件名(用于生成对象Key的后缀与元数据)
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件总大小(字节);分片总数由服务端按分片大小计算
     */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    /**
     * MIME类型(可选,缺省 application/octet-stream)
     */
    private String contentType;

    /**
     * 分片大小(字节);可选,缺省 5MB
     */
    private Integer chunkSize;

}