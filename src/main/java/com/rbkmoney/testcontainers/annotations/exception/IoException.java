package com.rbkmoney.testcontainers.annotations.exception;

public class IoException extends RuntimeException {

    public IoException(String message, Throwable cause) {
        super(message, cause);
    }
}
