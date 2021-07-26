package com.rbkmoney.testcontainers.annotations.exception;

public class NoSuchFileException extends RuntimeException {

    public NoSuchFileException(String message) {
        super(message);
    }
}
