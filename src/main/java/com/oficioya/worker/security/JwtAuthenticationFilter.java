package com.oficioya.worker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee "Authorization: Bearer <token>" y, si el token es válido, deja autenticado al usuario.
 * El nombre de la autenticación es el id del usuario (sub), que es lo que compara WorkerOwnership.
 *
 * No es @Component a propósito: si lo fuera, Spring Boot también lo registraría como filtro
 * de servlet fuera de la cadena de seguridad. Se crea en SecurityConfig.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Atributo donde se deja el motivo del rechazo para que el entry point lo devuelva en el 401
    public static final String AUTH_ERROR_ATTRIBUTE = "oficioya.jwt.error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sin token se sigue sin autenticar; SecurityConfig decide si la ruta lo exige
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = jwtService.validateToken(header.substring(BEARER_PREFIX.length()).trim());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    claims.userId().toString(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (InvalidTokenException ex) {
            // Token inválido: no se autentica y el entry point responde 401 con este motivo
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}
