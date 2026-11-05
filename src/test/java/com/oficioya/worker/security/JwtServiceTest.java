package com.oficioya.worker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "clave-de-pruebas-oficioya-worker-no-usar-en-produccion";
    private static final String OTHER_SECRET = "otra-clave-distinta-de-32-caracteres-o-mas-123";
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(60), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Genera un token válido con sub, role y expiración")
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken(10L, "TRABAJADOR");

        JwtClaims claims = jwtService.validateToken(token);

        assertEquals(10L, claims.userId());
        assertEquals("TRABAJADOR", claims.role());
        assertEquals(NOW.plus(Duration.ofMinutes(60)), claims.expiresAt());
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Los métodos extract devuelven id, rol y expiración del token")
    void shouldExtractClaims() {
        String token = jwtService.generateToken(7L, "ADMIN");

        assertEquals(7L, jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals(NOW.plus(Duration.ofMinutes(60)), jwtService.extractExpiration(token));
    }

    @Test
    @DisplayName("Normaliza el rol: acepta minúsculas y el prefijo ROLE_")
    void shouldNormalizeRole() {
        assertEquals("CONTRATANTE", jwtService.extractRole(jwtService.generateToken(1L, "contratante")));
        assertEquals("ADMIN", jwtService.extractRole(jwtService.generateToken(1L, "ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Rechaza un token expirado")
    void shouldRejectExpiredToken() {
        JwtService pastService = new JwtService(SECRET, Duration.ofMinutes(5),
                Clock.fixed(NOW.minus(Duration.ofHours(1)), ZoneOffset.UTC));
        String token = pastService.generateToken(10L, "TRABAJADOR");

        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
        assertEquals("Token has expired", ex.getMessage());
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Rechaza un token con el payload alterado")
    void shouldRejectTamperedPayload() {
        String token = jwtService.generateToken(10L, "TRABAJADOR");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"10\",\"role\":\"ADMIN\",\"exp\":" + NOW.plusSeconds(3600).getEpochSecond() + "}")
                        .getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(tampered));
        assertEquals("Token signature is invalid", ex.getMessage());
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave")
    void shouldRejectTokenSignedWithOtherKey() {
        String token = new JwtService(OTHER_SECRET, Duration.ofMinutes(60), Clock.fixed(NOW, ZoneOffset.UTC))
                .generateToken(10L, "ADMIN");

        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Rechaza tokens mal formados, vacíos o nulos")
    void shouldRejectMalformedTokens() {
        assertEquals("Token is malformed",
                assertThrows(InvalidTokenException.class, () -> jwtService.validateToken("no.es.un.jwt")).getMessage());
        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken("abc"));
        assertEquals("Token is missing",
                assertThrows(InvalidTokenException.class, () -> jwtService.validateToken("  ")).getMessage());
        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(null));
    }

    @Test
    @DisplayName("Rechaza un token firmado correctamente pero sin el claim role")
    void shouldRejectTokenWithoutRole() {
        String token = Jwts.builder()
                .subject("10")
                .expiration(Date.from(NOW.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals("Token must contain the claims sub and role",
                assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token)).getMessage());
    }

    @Test
    @DisplayName("Rechaza un token cuyo sub no es un id numérico")
    void shouldRejectNonNumericSubject() {
        String token = Jwts.builder()
                .subject("correo@ejemplo.com")
                .claim("role", "TRABAJADOR")
                .expiration(Date.from(NOW.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("No arranca con una clave de menos de 32 bytes")
    void shouldRejectShortSecret() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("corta", Duration.ofMinutes(60), Clock.systemUTC()));
        assertThrows(IllegalStateException.class,
                () -> new JwtService(null, Duration.ofMinutes(60), Clock.systemUTC()));
    }

    @Test
    @DisplayName("No genera tokens sin id o sin rol")
    void shouldNotGenerateTokenWithoutData() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(null, "ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(1L, " "));
    }
}
