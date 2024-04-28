package com.slm.security.exception;

import com.slm.security.enums.ResultStatus;
import com.slm.security.model.ApiResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ApiResponse<String> handler(BadCredentialsException e) {
        return ApiResponse.failure(ResultStatus.LOGIN_ERROR, e.getMessage());
    }

}
