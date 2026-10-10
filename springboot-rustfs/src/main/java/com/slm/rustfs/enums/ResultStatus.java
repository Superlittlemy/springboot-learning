package com.slm.rustfs.enums;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结果状态码
 * 
 * <p>业务 code 分段建议（便于管理和排查）：</p>
 *
 * <ul>
 * <li>0：成功</li>
 * <li>1xxx：通用错误（如参数错误、签名错误）</li>
 * <li>2xxx：用户/权限相关（如未登录、无权限、账号冻结）</li>
 * <li>3xxx：业务逻辑错误（如库存不足、订单已支付）</li>
 * <li>4xxx：第三方服务错误（如短信发送失败、支付渠道异常）</li>
 * <li>5xxx：服务端内部错误（数据库、缓存、文件系统）</li>
 * </ul>
 */
@Getter
@AllArgsConstructor 
public enum ResultStatus {

    SUCCESS(0, "调用成功", HttpStatus.OK),

    // 通用
    BAD_REQUEST(1000, "参数校验失败", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(1001, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    SOURCE_NOT_FOUND(1002, "资源不存在", HttpStatus.NOT_FOUND),

    // 业务
    FILE_SIZE_EXCEEDED(3003, "文件大小超出限制", HttpStatus.PAYLOAD_TOO_LARGE),
    UPLOAD_TASK_NOT_FOUND(3004, "上传任务不存在或已完成", HttpStatus.NOT_FOUND),
    CHUNK_SIZE_INVALID(3005, "分片大小不合法", HttpStatus.BAD_REQUEST),
    FILE_MD5_INVALID(3006, "MD5格式不合法", HttpStatus.BAD_REQUEST),

    // 系统
    ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    MINIO_OPERATION_ERROR(5001, "RustFS操作失败", HttpStatus.SERVICE_UNAVAILABLE),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

}
