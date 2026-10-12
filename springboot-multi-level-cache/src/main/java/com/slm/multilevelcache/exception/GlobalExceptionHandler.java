package com.slm.multilevelcache.exception;

import com.slm.multilevelcache.enums.ResultStatus;
import com.slm.multilevelcache.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * <p>注意：命中接入层缓存的响应由 Nginx 直接返回，不会经过这里。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：按 CacheException 携带的状态码与提示返回
     */
    @ExceptionHandler(CacheException.class)
    public ResponseEntity<ApiResponse<String>> handler(CacheException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseEntity.status(e.getStatus().getHttpStatus())
                .body(ApiResponse.failure(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ResultStatus.BAD_REQUEST.getMessage());
        return ResponseEntity.status(ResultStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handler(Exception e) {
        log.error("未知异常：{}", e.getMessage(), e);
        return ResponseEntity.status(ResultStatus.ERROR.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.ERROR, ResultStatus.ERROR.getMessage()));
    }

}