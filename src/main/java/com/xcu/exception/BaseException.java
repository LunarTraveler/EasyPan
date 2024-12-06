package com.xcu.exception;

/**
 * 业务异常
 */
public class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }

    /**
     * 重写 fillInStackTrace 业务异常不需要堆栈信息，提高效率
     */
    @Override
    public Throwable fillInStackTrace(){
        return this;
    }

}
