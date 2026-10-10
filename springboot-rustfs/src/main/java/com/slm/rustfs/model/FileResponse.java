package com.slm.rustfs.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件响应
 */
@Data
public class FileResponse {

    /** 文件ID */
    private Long id;

    /** 原始文件名 */
    private String originalName;

    /** MIME类型 */
    private String contentType;

    /** 文件大小(字节) */
    private Long sizeBytes;

    /** MD5(小写hex) */
    private String md5;

    /** 对象Key */
    private String objectKey;

    /** 创建时间 */
    private LocalDateTime createdTime;

}
