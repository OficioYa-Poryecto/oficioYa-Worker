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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "clave-de-pruebas-oficioya-worker-no-usar-en-produccion";
    private static final String OTHER_SECRET = "otra-clave-distinta-de-32-caracteres-o-mas-123";
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(60), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Genera un token válido con sub, roles y expiración")
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken(USER_ID, List.of("TRABAJADOR"));

        JwtClaims claims = jwtService.validateToken(token);

        assertEquals(USER_ID, claims.userId());
        assertEquals(Set.of("TRABAJADOR"), claims.roles());
        assertEquals(NOW.plus(Duration.ofMinutes(60)), claims.expiresAt());
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Los métodos extract devuelven id, rol y expiración del token")
    void shouldExtractClaims() {
        String token = jwtService.generateToken(USER_ID, List.of("ADMIN"));

        assertEquals(USER_ID, jwtService.extractUserId(token));
        assertEquals(Set.of("ADMIN"), jwtService.extractRoles(token));
        assertEquals(NOW.plus(Duration.ofMinutes(60)), jwtService.extractExpiration(token));
    }

    @Test
    @DisplayName("Normaliza el rol: acepta minúsculas y el prefijo ROLE_")
    void shouldNormalizeRole() {
        assertEquals(Set.of("CONTRATANTE"),
                jwtService.extractRoles(jwtService.generateToken(USER_ID, List.of("contratante"))));
        assertEquals(Set.of("ADMIN"),
                jwtService.extractRoles(jwtService.generateToken(USER_ID, List.of("ROLE_ADMIN"))));
    }

    @Test
    @DisplayName("Rechaza un token expirado")
    void shouldRejectExpiredToken() {
        JwtService pastService = new JwtService(SECRET, Duration.ofMinutes(5),
                Clock.fixed(NOW.minus(Duration.ofHours(1)), ZoneOffset.UTC));
        String token = pastService.generateToken(USER_ID, List.of("TRABAJADOR"));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
        assertEquals("Token has expired", ex.getMessage());
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Rechaza un token con el payload alterado")
    void shouldRejectTamperedPayload() {
        String token = jwtService.generateToken(USER_ID, List.of("TRABAJADOR"));
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + USER_ID + "\",\"roles\":[\"ADMIN\"],\"exp\":"
                        + NOW.plusSeconds(3600).getEpochSecond() + "}")
                        .getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(tampered));
        assertEquals("Token signature is invalid", ex.getMessage());
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave")
    void shouldRejectTokenSignedWithOtherKey() {
        String token = new JwtService(OTHER_SECRET, Duration.ofMinutes(60), Clock.fixed(NOW, ZoneOffset.UTC))
                .generateToken(USER_ID, List.of("ADMIN"));

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
    @DisplayName("Rechaza un token firmado correctamente pero sin el claim roles")
    void shouldRejectTokenWithoutRoles() {
        String token = Jwts.builder()
                .subject(USER_ID.toString())
                .expiration(Date.from(NOW.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals("Token must contain the claims sub and roles",
                assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token)).getMessage());
    }

    @Test
    @DisplayName("Rechaza un token cuyo sub no es un UUID")
    void shouldRejectNonUuidSubject() {
        String token = Jwts.builder()
                .subject("correo@ejemplo.com")
                .claim(JwtService.ROLES_CLAIM, List.of("TRABAJADOR"))
                .expiration(Date.from(NOW.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Rechaza un token firmado cuyo subject no representa un UUID")
    void shouldRejectSubjectThatIsNotUuid() {
        String token = Jwts.builder()
                .subject("not-a-uuid")
                .claim(JwtService.ROLES_CLAIM, List.of("TRABAJADOR"))
                .expiration(Date.from(NOW.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Rechaza un token con el formato antiguo de claim role")
    void shouldRejectTokenWithLegacyRoleClaim() {
        String token = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("role", "TRABAJADOR")
                .expiration(Date.from(NOW.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(InvalidTokenException.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Acepta un token con múltiples roles")
    void shouldAcceptMultipleRoles() {
        String token = jwtService.generateToken(USER_ID, List.of("TRABAJADOR", "ADMIN"));

        JwtClaims claims = jwtService.validateToken(token);

        assertEquals(Set.of("TRABAJADOR", "ADMIN"), claims.roles());
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
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(null, List.of("ADMIN")));
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(USER_ID, List.of()));
    }
}
