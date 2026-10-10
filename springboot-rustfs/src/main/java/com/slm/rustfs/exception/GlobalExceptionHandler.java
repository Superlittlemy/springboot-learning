package com.slm.rustfs.exception;

import com.slm.rustfs.enums.ResultStatus;
import com.slm.rustfs.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        log.error("未知异常：{}", e.getMessage(), e);
        return ApiResponse.failure(ResultStatus.ERROR, ResultStatus.ERROR.getMessage());
    }

}
