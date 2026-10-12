package com.slm.multilevelcache.exception;

import com.slm.multilevelcache.enums.ResultStatus;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class CacheException extends RuntimeException {

    private final ResultStatus status;

    public CacheException(ResultStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public CacheException(ResultStatus status, String message) {
        super(message);
        this.status = status;
    }

    public CacheException(ResultStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

}