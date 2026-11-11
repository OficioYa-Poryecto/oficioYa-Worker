package com.oficioya.worker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// Genera tokens solo para pruebas; el servicio no emite tokens (los emite Identity)
public final class TestTokens {

    private TestTokens() {
    }

    public static String generate(String base64Secret, Clock clock, Duration validity,
                                  UUID userId, String... roles) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(JwtService.ROLES_CLAIM, List.of(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret)), Jwts.SIG.HS256)
                .compact();
    }

    // Token válido por 60 minutos con el reloj del sistema
    public static String generate(String base64Secret, UUID userId, String... roles) {
        return generate(base64Secret, Clock.systemUTC(), Duration.ofMinutes(60), userId, roles);
    }
}