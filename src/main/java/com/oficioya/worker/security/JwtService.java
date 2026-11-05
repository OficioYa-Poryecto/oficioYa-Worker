package com.oficioya.worker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

/**
 * Genera y valida tokens JWT firmados con HS256.
 *
 * Supuesto pendiente de confirmar con Identity: el claim "sub" trae el id numérico del usuario
 * y el claim "role" trae el rol (TRABAJADOR, CONTRATANTE o ADMIN). Si Identity usa otros nombres,
 * solo hay que cambiar las constantes y toClaims().
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    static final String ROLE_CLAIM = "role";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final int MIN_SECRET_LENGTH = 32;   // 256 bits, lo mínimo para HS256

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this(secret, Duration.ofMinutes(expirationMinutes), Clock.systemUTC());
    }

    // El reloj se inyecta para poder probar tokens expirados
    JwtService(String secret, Duration expiration, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("security.jwt.secret must have at least " + MIN_SECRET_LENGTH + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.clock = clock;
    }

    public String generateToken(Long userId, String role) {
        if (userId == null || role == null || role.isBlank()) {
            throw new IllegalArgumentException("User id and role are required to generate a token");
        }
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, normalizeRole(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Valida firma y expiración y devuelve los claims; lanza InvalidTokenException si algo falla
    public JwtClaims validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is missing");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return toClaims(claims);
        } catch (ExpiredJwtException ex) {
            log.debug("Expired token for subject {}", ex.getClaims().getSubject());
            throw new InvalidTokenException("Token has expired", ex);
        } catch (SignatureException ex) {
            log.warn("Token with invalid signature");
            throw new InvalidTokenException("Token signature is invalid", ex);
        } catch (MalformedJwtException ex) {
            throw new InvalidTokenException("Token is malformed", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token is invalid", ex);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (InvalidTokenException ex) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return validateToken(token).userId();
    }

    public String extractRole(String token) {
        return validateToken(token).role();
    }

    public Instant extractExpiration(String token) {
        return validateToken(token).expiresAt();
    }

    // Único punto donde se leen los claims del token
    private JwtClaims toClaims(Claims claims) {
        String subject = claims.getSubject();
        String role = claims.get(ROLE_CLAIM, String.class);
        if (subject == null || role == null || role.isBlank()) {
            throw new InvalidTokenException("Token must contain the claims sub and role");
        }
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new InvalidTokenException("Token subject is not a valid user id", ex);
        }
        Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
        return new JwtClaims(userId, normalizeRole(role), expiresAt);
    }

    // Acepta "trabajador", "TRABAJADOR" o "ROLE_TRABAJADOR" y devuelve "TRABAJADOR"
    private String normalizeRole(String role) {
        String upper = role.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith(ROLE_PREFIX) ? upper.substring(ROLE_PREFIX.length()) : upper;
    }
}
