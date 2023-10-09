package com.slm.rediscache.exception;

import lombok.Data;

/**
 * 业务异常
 */
@Data
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

}
