package com.slm.rustfs.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import lombok.Data;

/**
 * 闪传探测请求
 */
@Data
public class InstantUploadRequest {

    /**
     * 客户端计算出的整文件 MD5(hex);大小写均可,服务端统一按小写匹配
     */
    @NotBlank(message = "MD5不能为空")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "MD5必须是32位十六进制字符串")
    private String md5;

    /**
     * 文件大小(字节);传入时会与历史记录的大小比对,不一致按未命中处理
     */
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    /**
     * 文件名(可选);不参与命中判断,仅便于服务端日志排查
     */
    private String fileName;

}
