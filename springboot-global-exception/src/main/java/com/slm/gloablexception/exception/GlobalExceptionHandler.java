package com.slm.gloablexception.exception;

import com.slm.gloablexception.enums.ResultStatus;
import com.slm.gloablexception.model.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

}
