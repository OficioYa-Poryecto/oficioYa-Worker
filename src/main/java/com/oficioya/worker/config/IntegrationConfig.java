package com.oficioya.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class IntegrationConfig {

    // RestClient hacia el Orquestador; el builder ya trae Jackson y los timeouts de spring.http.client.*
    @Bean
    RestClient orchestratorHttpClient(RestClient.Builder builder,
                                      @Value("${integration.orchestrator.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}