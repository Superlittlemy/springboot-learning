package com.slm.restapi.enums;

import lombok.Getter;

/**
 * 结果状态码
 */
@Getter
public enum ResultStatus {

    SUCCESS(20000, "调用成功"),
    ERROR(50000, "接口异常");

    private int code;
    private String message;

    ResultStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
