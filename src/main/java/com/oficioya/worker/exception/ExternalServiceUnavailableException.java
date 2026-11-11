package com.oficioya.worker.exception;

public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
