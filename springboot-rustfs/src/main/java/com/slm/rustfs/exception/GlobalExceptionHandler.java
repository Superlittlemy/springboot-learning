package com.slm.rustfs.exception;

import com.slm.rustfs.enums.ResultStatus;
import com.slm.rustfs.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常:按 FileException 携带的状态码与提示返回,便于前端展示具体原因
     */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<ApiResponse<String>> handler(FileException e) {
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handler(MaxUploadSizeExceededException e) {
        log.warn("上传体积超出限制：{}", e.getMessage());
        return ResponseEntity.status(ResultStatus.FILE_SIZE_EXCEEDED.getHttpStatus())
            .body(ApiResponse.failure(ResultStatus.FILE_SIZE_EXCEEDED, "单个分片超出服务端允许的最大上传体积"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handler(Exception e) {
        log.error("未知异常：{}", e.getMessage(), e);
        return ResponseEntity.status(ResultStatus.ERROR.getHttpStatus())
            .body(ApiResponse.failure(ResultStatus.ERROR, ResultStatus.ERROR.getMessage()));
    }

}
