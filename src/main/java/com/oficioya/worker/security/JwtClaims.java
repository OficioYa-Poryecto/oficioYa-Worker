package com.oficioya.worker.security;

import java.time.Instant;

// Datos que el dominio Worker usa del token: id del usuario (sub), rol (sin prefijo ROLE_) y expiración
public record JwtClaims(Long userId, String role, Instant expiresAt) {
}
