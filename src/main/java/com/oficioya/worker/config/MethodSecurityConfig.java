package com.oficioya.worker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// Activa @PreAuthorize en los controladores. La cadena de filtros y el filtro JWT van en SecurityConfig.
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
