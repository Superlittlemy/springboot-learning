package com.slm.validator.exception;

import com.slm.validator.enums.ResultStatus;
import com.slm.validator.model.ApiResponse;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public ApiResponse handler(Exception e) {
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handler(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder sb = new StringBuilder("参数校验失败：");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append(fieldError.getDefaultMessage()).append("(").append(fieldError.getField()).append(")，");
        }
        return ApiResponse.failure(ResultStatus.ERROR, sb.deleteCharAt(sb.length() - 1));
    }

}
