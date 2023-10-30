package com.slm.mybatisplus.enums;

import lombok.Getter;

/**
 * 结果状态码
 */
@Getter
public enum ResultStatus {

    SUCCESS(20000, "调用成功"),
    REJECT(30000, "请求拒绝"),
    ERROR(50000, "接口异常");

    private final int code;
    private final String message;

    ResultStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
