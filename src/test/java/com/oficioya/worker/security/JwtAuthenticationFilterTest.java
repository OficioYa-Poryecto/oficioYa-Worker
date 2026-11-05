package com.oficioya.worker.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "clave-de-pruebas-oficioya-worker-no-usar-en-produccion";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(60), Clock.systemUTC());
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Con un token válido autentica al usuario con su id como nombre y su rol")
    void shouldAuthenticateWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(10L, "TRABAJADOR"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("10", auth.getName());
        assertTrue(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_TRABAJADOR"::equals));
        assertNotNull(chain.getRequest(), "la cadena debe continuar");
    }

    @Test
    @DisplayName("Sin header Authorization continúa la cadena sin autenticar")
    void shouldContinueWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE));
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Un header que no es Bearer se ignora")
    void shouldIgnoreNonBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Con un token inválido no autentica y deja el motivo para el 401")
    void shouldNotAuthenticateWithInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-falso");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("Token is malformed", request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE));
        assertNotNull(chain.getRequest(), "la cadena continúa y el entry point decide el 401");
    }
}
