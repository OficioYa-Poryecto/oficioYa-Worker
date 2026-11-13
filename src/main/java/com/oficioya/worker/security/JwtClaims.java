package com.oficioya.worker.security;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;

// Datos que el dominio Worker usa del token: id del usuario (sub), rol (sin prefijo ROLE_) y expiración
public record JwtClaims(UUID userId, Set<String> roles, Instant expiresAt) {
}
