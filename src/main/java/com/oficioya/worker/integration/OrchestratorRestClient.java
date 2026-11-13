package com.oficioya.worker.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oficioya.worker.exception.ExternalServiceUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Habla con Users a través del Orquestador. Rutas provisionales: /zones y /users.
@Component
public class OrchestratorRestClient implements ZoneCatalogClient, UserDirectoryClient {

    private static final int BATCH_SIZE = 50;

    private final RestClient restClient;

    public OrchestratorRestClient(RestClient orchestratorHttpClient) {
        this.restClient = orchestratorHttpClient;
    }

    @Override
    public boolean existAll(Set<Long> zoneIds) {
        if (zoneIds.isEmpty()) {
            return true;
        }
        String ids = zoneIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ZoneResponse[] found = call("zones", () -> restClient.get()
                .uri(uri -> uri.path("/zones").queryParam("ids", ids).build())
                .retrieve()
                .body(ZoneResponse[].class));
        if (found == null) {
            return false;
        }
        Set<Long> foundIds = Arrays.stream(found).map(ZoneResponse::id).collect(Collectors.toSet());
        return foundIds.containsAll(zoneIds);
    }

    @Override
    public Set<UUID> findActiveIds(Collection<UUID> userIds) {
        List<UUID> ids = userIds.stream().distinct().toList();
        String authorization = currentAuthorization();
        Set<UUID> active = new HashSet<>();

        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            String batch = ids.subList(i, Math.min(i + BATCH_SIZE, ids.size())).stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            UserResponse[] found = call("users", () -> restClient.get()
                    .uri(uri -> uri.path("/users").queryParam("ids", batch).build())
                    .headers(headers -> {
                        if (authorization != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .retrieve()
                    .body(UserResponse[].class));
            if (found != null) {
                for (UserResponse user : found) {
                    active.add(user.id());
                }
            }
        }
        return active;
    }

    // Token del usuario que hizo la petición actual (null si no hay petición HTTP en curso)
    private String currentAuthorization() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }

    private <T> T call(String resource, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException ex) {
            throw new ExternalServiceUnavailableException("Users service unavailable (" + resource + ")", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ZoneResponse(Long id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserResponse(UUID id) {
    }
}