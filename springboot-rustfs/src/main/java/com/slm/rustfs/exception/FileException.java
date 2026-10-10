package com.slm.rustfs.exception;

import com.slm.rustfs.enums.ResultStatus;

import lombok.Getter;

/**
 * 文件服务异常
 */
@Getter
public class FileException extends RuntimeException {

    private final ResultStatus status;
    private final String message;

    public FileException(ResultStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
