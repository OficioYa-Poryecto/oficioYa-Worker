package com.oficioya.worker.security;

import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.model.WorkerStatus;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.WorkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Prueba la cadena completa: filtro JWT real, entry point (401), access denied handler (403)
// y que el resto de errores salgan con el mismo formato.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityErrorHandlingTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    TradeCategoryRepository categoryRepository;

    @Autowired
    WorkerRepository workerRepository;

    @Value("${security.jwt.secret}")
    String secret;

    @Test
    @DisplayName("Sin token: 401 con el formato común de error")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/trades"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/trades"));
    }

    @Test
    @DisplayName("Token mal formado: 401 con el motivo")
    void tokenInvalidoResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/trades").header("Authorization", "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is malformed"));
    }

    @Test
    @DisplayName("Token expirado: 401 con el motivo")
    void tokenExpiradoResponde401() throws Exception {
        String expired = new JwtService(secret, Duration.ofMinutes(1),
                Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC))
                .generateToken(10L, "TRABAJADOR");

        mockMvc.perform(get("/api/v1/trades").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    @Test
    @DisplayName("Token firmado con otra clave: 401")
    void tokenConOtraClaveResponde401() throws Exception {
        String forged = new JwtService("clave-falsa-de-un-atacante-con-32-caracteres", Duration.ofMinutes(60),
                Clock.systemUTC()).generateToken(1L, "ADMIN");

        mockMvc.perform(get("/api/v1/trades").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token signature is invalid"));
    }

    @Test
    @DisplayName("Token válido con rol insuficiente: 403 con el formato común de error")
    void rolInsuficienteResponde403() throws Exception {
        mockMvc.perform(post("/api/v1/trade-categories")
                        .header("Authorization", bearer(10L, "TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hogar\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path").value("/api/v1/trade-categories"));
    }

    @Test
    @DisplayName("Token válido de ADMIN: crea categorías a través del filtro real")
    void adminConTokenCreaCategoria() throws Exception {
        mockMvc.perform(post("/api/v1/trade-categories")
                        .header("Authorization", bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Tecnología\", \"detail\": \"Reparación de equipos\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Tecnología"));
    }

    @Test
    @DisplayName("Token válido de CONTRATANTE: puede consultar el catálogo")
    void contratanteConsultaCatalogo() throws Exception {
        mockMvc.perform(get("/api/v1/trades").header("Authorization", bearer(20L, "CONTRATANTE")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Swagger/OpenAPI es público")
    void swaggerEsPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Recurso inexistente: 404 con el mismo formato que 401 y 403")
    void noEncontradoUsaElMismoFormato() throws Exception {
        mockMvc.perform(get("/api/v1/trades/999999").header("Authorization", bearer(20L, "CONTRATANTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Trade not found: 999999"))
                .andExpect(jsonPath("$.path").value("/api/v1/trades/999999"));
    }

    @Test
    @DisplayName("Categoría duplicada: 409 con el mismo formato")
    void duplicadoUsaElMismoFormato() throws Exception {
        TradeCategory hogar = new TradeCategory();
        hogar.setName("Hogar");
        categoryRepository.save(hogar);

        mockMvc.perform(post("/api/v1/trade-categories")
                        .header("Authorization", bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"hogar\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Trade category already exists: hogar"));
    }

    @Test
    @DisplayName("Datos inválidos en el DTO: 400 indicando el campo")
    void validacionResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/trade-categories")
                        .header("Authorization", bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    @DisplayName("El perfil público del trabajador no expone correo ni teléfono")
    void perfilNoExponeDatosDeContacto() throws Exception {
        Worker worker = new Worker();
        worker.setUserId(10L);
        worker.setStatus(WorkerStatus.ACTIVE);
        Long workerId = workerRepository.save(worker).getId();

        mockMvc.perform(get("/api/v1/workers/" + workerId).header("Authorization", bearer(20L, "CONTRATANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerId))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(content().string(not(containsString("phone"))));
    }

    private String bearer(Long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
