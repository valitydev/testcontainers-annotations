package com.rbkmoney.testcontainers.annotations.exception;

public class ClickhouseStartingException extends RuntimeException {

    public ClickhouseStartingException(String message, Throwable cause) {
        super(message, cause);
    }
}
