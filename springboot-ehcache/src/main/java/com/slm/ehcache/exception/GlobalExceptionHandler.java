package com.slm.ehcache.exception;

import com.slm.ehcache.enums.ResultStatus;
import com.slm.ehcache.model.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<String> handler(BizException e) {
        return ApiResponse.failure(ResultStatus.REJECT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        e.printStackTrace();
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

}
