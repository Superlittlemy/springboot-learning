package com.slm.jpa.model;

import com.slm.jpa.enums.ResultStatus;
import lombok.Data;

/**
 * 统一返回对象
 *
 * @param <T>
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 调用成功（无数据返回）
     */
    public static ApiResponse<?> ok() {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getMessage(), null);
    }

    /**
     * 调用成功（数据返回）
     */
    public static <T> ApiResponse<T> ok(T data) {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getMessage(), data);
    }

    /**
     * 调用失败
     */
    public static ApiResponse<?> failure(ResultStatus status) {
        return new ApiResponse<>(status.getCode(), status.getMessage(), null);
    }

    /**
     * 调用失败（附带详细描述）
     */
    public static <T> ApiResponse<T> failure(ResultStatus status, T data) {
        return new ApiResponse<>(status.getCode(), status.getMessage(), data);
    }

}
