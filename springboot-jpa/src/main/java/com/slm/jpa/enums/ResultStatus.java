package com.slm.jpa.enums;

import lombok.Getter;

/**
 * 结果状态码
 */
@Getter
public enum ResultStatus {

    SUCCESS(20000, "成功"),
    REJECT(30000, "请求拒绝"),
    ERROR(50000, "失败");

    private final int code;
    private final String message;

    ResultStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
