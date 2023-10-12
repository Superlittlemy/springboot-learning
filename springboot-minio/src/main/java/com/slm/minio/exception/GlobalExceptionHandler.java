package com.slm.minio.exception;

import com.slm.minio.enums.ResultStatus;
import com.slm.minio.model.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public ApiResponse handler(Exception e) {
        e.printStackTrace();
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

}
