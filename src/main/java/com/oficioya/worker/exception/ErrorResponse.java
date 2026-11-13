package com.oficioya.worker.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

// Cuerpo comun de los errores: lo usan el @RestControllerAdvice y los handlers 401/403 de seguridad
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
