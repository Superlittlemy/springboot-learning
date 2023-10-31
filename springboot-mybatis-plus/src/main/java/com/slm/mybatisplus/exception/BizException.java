package com.slm.mybatisplus.exception;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

}
