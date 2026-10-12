package com.slm.multilevelcache.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 结果状态码
 *
 * <p>业务 code 分段沿用 springboot-rustfs 的约定，便于统一排查：</p>
 *
 * <ul>
 * <li>0：成功</li>
 * <li>1xxx：通用错误（如参数错误、资源不存在）</li>
 * <li>3xxx：业务逻辑错误</li>
 * <li>5xxx：服务端内部错误（含缓存层）</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ResultStatus {

    SUCCESS(0, "调用成功", HttpStatus.OK),

    // 通用
    BAD_REQUEST(1000, "参数校验失败", HttpStatus.BAD_REQUEST),
    SOURCE_NOT_FOUND(1002, "资源不存在", HttpStatus.NOT_FOUND),

    // 业务
    PRODUCT_NOT_FOUND(3001, "商品不存在", HttpStatus.NOT_FOUND),
    PRODUCT_UPDATE_CONFLICT(3002, "商品更新冲突", HttpStatus.CONFLICT),

    // 系统
    ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    CACHE_OPERATION_ERROR(5001, "缓存操作失败", HttpStatus.SERVICE_UNAVAILABLE),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

}