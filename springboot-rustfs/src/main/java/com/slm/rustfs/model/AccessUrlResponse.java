package com.slm.rustfs.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件访问地址响应
 */
@Data
public class AccessUrlResponse {

    /** 对象Key */
    private String objectKey;

    /** 原始文件名 */
    private String originalName;

    /** 带过期时间的访问地址(GET预签名URL) */
    private String accessUrl;

    /** 访问地址过期时间 */
    private LocalDateTime expiresAt;

}
