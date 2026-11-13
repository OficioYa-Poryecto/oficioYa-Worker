package com.oficioya.worker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API OficioYa - Dominio Worker")
                        .version("1.0.0")
                        .description("Documentación interactiva de la API de trabajadores, oficios, zonas, disponibilidad y fotos de OficioYa"));
    }
}