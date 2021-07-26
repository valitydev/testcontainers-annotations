package com.rbkmoney.testcontainers.annotations.exception;

public class KafkaStartingException extends RuntimeException {

    public KafkaStartingException(String message, Throwable cause) {
        super(message, cause);
    }
}
