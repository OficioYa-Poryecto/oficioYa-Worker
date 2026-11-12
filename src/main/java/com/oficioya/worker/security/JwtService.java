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
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera y valida tokens JWT firmados con HS256.
 *
 * JWT_SECRET viene en Base64 y que se decodifica
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    static final String ROLES_CLAIM = "roles";
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
        this.key = Keys.hmacShaKeyFor(decodeSecret(secret));
        this.expiration = expiration;
        this.clock = clock;
    }

    public String generateToken(UUID userId, Collection<String> roles) {
        if (userId == null || roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("User id and roles are required to generate a token");
        }
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLES_CLAIM, roles.stream().map(this::normalizeRole).toList())
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

    public UUID extractUserId(String token) {
        return validateToken(token).userId();
    }

    public Set<String> extractRoles(String token) {
        return validateToken(token).roles();
    }

    public Instant extractExpiration(String token) {
        return validateToken(token).expiresAt();
    }

    // Único punto donde se leen los claims del token
    private JwtClaims toClaims(Claims claims) {
        String subject = claims.getSubject();
        List<?> rawRoles = claims.get(ROLES_CLAIM, List.class);
        if (subject == null || rawRoles == null || rawRoles.isEmpty()) {
            throw new InvalidTokenException("Token must contain the claims sub and roles");
        }
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Token subject is not a valid user id", ex);
        }
        Set<String> roles = rawRoles.stream()
                .map(Object::toString).map(this::normalizeRole).collect(Collectors.toSet());
        Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
        return new JwtClaims(userId, roles, expiresAt);
    }

    // Acepta "trabajador", "TRABAJADOR" o "ROLE_TRABAJADOR" y devuelve "TRABAJADOR"
    private String normalizeRole(String role) {
        String upper = role.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith(ROLE_PREFIX) ? upper.substring(ROLE_PREFIX.length()) : upper;
    }

    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret is required");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("security.jwt.secret must be Base64", ex);
        }
        if (bytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "security.jwt.secret must have at least " + MIN_SECRET_LENGTH + " bytes once decoded");
        }
        return bytes;
    }
}
