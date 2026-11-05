package com.oficioya.worker.security;

import org.springframework.security.core.AuthenticationException;

// Token ausente de claims, expirado, mal formado o con firma inválida. Termina en un 401.
public class InvalidTokenException extends AuthenticationException {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
