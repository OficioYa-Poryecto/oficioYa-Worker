package com.oficioya.worker.controller;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.model.WorkerStatus;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.TradeRepository;
import com.oficioya.worker.repository.WorkerRepository;
import com.oficioya.worker.security.JwtService;
import com.oficioya.worker.security.TestTokens;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El username de @WithMockUser es el id del usuario, igual que el "sub" del JWT
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkerAuthorizationTest {

    private static final String DUENO_NAME = "82b9fd87-5355-4247-bc2c-9bce2a35ee8c";
    private static final String OTRO_NAME = "d1f6d739-881c-4486-ae42-555f31fa3521";
    private static final String CONTRATANTE_NAME = "64943221-49be-4491-83fd-1aadd44fc160";
    private static final String ADMIN_NAME = "a057fe75-2e5e-46c9-b7d9-afdf8f730110";
    private static final UUID DUENO = UUID.fromString(DUENO_NAME);
    private static final UUID CONTRATANTE = UUID.fromString(CONTRATANTE_NAME);
    private static final UUID SERVICE_ID = UUID.fromString("5e2c1f0a-7b3d-4c9e-8a1f-2d6b9e4c7a10");
    private static final long ZONA = 5L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WorkerRepository workerRepository;

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    TradeCategoryRepository categoryRepository;

    @Autowired
    JwtService jwtService;

    private UUID workerDelDueno;
    private Trade plomeria;

    @Value("${security.jwt.secret}")
    String secret;

    @BeforeEach
    void preparar() {
        TradeCategory hogar = new TradeCategory();
        hogar.setName("Hogar");
        hogar = categoryRepository.save(hogar);

        plomeria = new Trade();
        plomeria.setName("Plomería");
        plomeria.setActive(true);
        plomeria.setCategory(hogar);
        plomeria = tradeRepository.save(plomeria);

        Worker worker = new Worker();
        worker.setUserId(DUENO);
        worker.setStatus(WorkerStatus.ACTIVE);
        workerDelDueno = workerRepository.save(worker).getId();
    }

    @Test
    @WithMockUser(username = DUENO_NAME, roles = "TRABAJADOR")
    @DisplayName("El trabajador dueño puede agregar una zona a su perfil")
    void duenoAgregaZona() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/zones/" + ZONA).with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(workerRepository.findById(workerDelDueno).orElseThrow().getZoneIds().contains(ZONA));
    }

    @Test
    @WithMockUser(username = OTRO_NAME, roles = "TRABAJADOR")
    @DisplayName("Otro trabajador no puede modificar un perfil ajeno: 403 y sin cambios")
    void otroTrabajadorNoModificaPerfilAjeno() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/zones/" + ZONA).with(csrf()))
                .andExpect(status().isForbidden());

        assertFalse(workerRepository.findById(workerDelDueno).orElseThrow().getZoneIds().contains(ZONA));
    }

    @Test
    @WithMockUser(username = DUENO_NAME, roles = "CONTRATANTE")
    @DisplayName("Un contratante no puede subir fotos al perfil de un trabajador: 403")
    void contratanteNoSubeFotos() throws Exception {
        mockMvc.perform(post("/api/v1/workers/" + workerDelDueno + "/photos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://fotos.example.com/1.jpg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crea un perfil asociado al usuario autenticado del token")
    void creaPerfilParaUsuarioAutenticado() throws Exception {
        workerRepository.deleteById(workerDelDueno);
        workerRepository.flush();

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", bearer(DUENO, "TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(DUENO.toString()));
    }

    @Test
    @DisplayName("Un contratante no puede crear un perfil de trabajador: 403")
    void contratanteNoCreaPerfil() throws Exception {
        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", bearer(CONTRATANTE, "CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = DUENO_NAME, roles = "TRABAJADOR")
    @DisplayName("Un trabajador no puede crear oficios en el catálogo: 403")
    void trabajadorNoCreaOficios() throws Exception {
        mockMvc.perform(post("/api/v1/trades").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oficioJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_NAME, roles = "ADMIN")
    @DisplayName("El administrador puede crear oficios en el catálogo: 201")
    void adminCreaOficios() throws Exception {
        mockMvc.perform(post("/api/v1/trades").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oficioJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = OTRO_NAME, roles = "CONTRATANTE")
    @DisplayName("Un contratante puede consultar el perfil público de un trabajador: 200")
    void contratanteConsultaPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/workers/" + workerDelDueno))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SERVICE obtiene el perfil por id de usuario aunque esté pausado: 200")
    void serviceObtienePerfilPausadoPorUsuario() throws Exception {
        Worker worker = workerRepository.findById(workerDelDueno).orElseThrow();
        worker.setStatus(WorkerStatus.PAUSED);
        workerRepository.save(worker);

        mockMvc.perform(get("/api/v1/workers/by-user/" + DUENO)
                        .header("Authorization", bearer(SERVICE_ID, "SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerDelDueno.toString()))
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    @DisplayName("ADMIN obtiene el perfil por id de usuario: 200")
    void adminObtienePerfilPorUsuario() throws Exception {
        mockMvc.perform(get("/api/v1/workers/by-user/" + DUENO)
                        .header("Authorization", bearer(UUID.fromString(ADMIN_NAME), "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(DUENO.toString()));
    }

    @Test
    @DisplayName("by-user responde 404 si el usuario no tiene perfil de trabajador")
    void byUserSinPerfilDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/workers/by-user/" + CONTRATANTE)
                        .header("Authorization", bearer(SERVICE_ID, "SERVICE")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un trabajador no puede usar by-user, ni siquiera con su propio id: 403")
    void trabajadorNoUsaByUser() throws Exception {
        mockMvc.perform(get("/api/v1/workers/by-user/" + DUENO)
                        .header("Authorization", bearer(DUENO, "TRABAJADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SERVICE puede pausar y reactivar un perfil: 200")
    void servicePausaYReactiva() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/status")
                        .header("Authorization", bearer(SERVICE_ID, "SERVICE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PAUSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/status")
                        .header("Authorization", bearer(SERVICE_ID, "SERVICE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("ADMIN puede pausar un perfil: 200")
    void adminPausa() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/status")
                        .header("Authorization", bearer(UUID.fromString(ADMIN_NAME), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PAUSED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ni el trabajador dueño ni un contratante pueden cambiar el estado: 403 y sin cambios")
    void trabajadorYContratanteNoCambianEstado() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/status")
                        .header("Authorization", bearer(DUENO, "TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PAUSED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/status")
                        .header("Authorization", bearer(CONTRATANTE, "CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PAUSED\"}"))
                .andExpect(status().isForbidden());

        assertEquals(WorkerStatus.ACTIVE, workerRepository.findById(workerDelDueno).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("GET /workers/me sigue siendo solo para TRABAJADOR: SERVICE recibe 403")
    void serviceNoUsaMe() throws Exception {
        mockMvc.perform(get("/api/v1/workers/me")
                        .header("Authorization", bearer(DUENO, "SERVICE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un contratante no ve el perfil público de un trabajador pausado: 404")
    void contratanteNoVePerfilPausado() throws Exception {
        pausarPerfilDelDueno();

        mockMvc.perform(get("/api/v1/workers/" + workerDelDueno)
                        .header("Authorization", bearer(CONTRATANTE, "CONTRATANTE")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El dueño, ADMIN y SERVICE sí ven un perfil pausado: 200")
    void duenoAdminYServiceVenPerfilPausado() throws Exception {
        pausarPerfilDelDueno();

        mockMvc.perform(get("/api/v1/workers/" + workerDelDueno)
                        .header("Authorization", bearer(DUENO, "TRABAJADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
        mockMvc.perform(get("/api/v1/workers/" + workerDelDueno)
                        .header("Authorization", bearer(UUID.fromString(ADMIN_NAME), "ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/workers/" + workerDelDueno)
                        .header("Authorization", bearer(SERVICE_ID, "SERVICE")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = DUENO_NAME, roles = "TRABAJADOR")
    @DisplayName("El dueño puede registrar una foto con la ruta relativa del servicio de imágenes: 201")
    void registraFotoConRutaRelativa() throws Exception {
        mockMvc.perform(post("/api/v1/workers/" + workerDelDueno + "/photos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"/api/v1/images/3f2b8c1e-9a4d-4e7f-b6c2-1d5e8f9a0b3c\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/api/v1/images/3f2b8c1e-9a4d-4e7f-b6c2-1d5e8f9a0b3c"));
    }

    @Test
    @WithMockUser(username = DUENO_NAME, roles = "TRABAJADOR")
    @DisplayName("Rechaza rutas relativas que no son del servicio de imágenes: 400")
    void rechazaRutaRelativaAjena() throws Exception {
        mockMvc.perform(post("/api/v1/workers/" + workerDelDueno + "/photos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"/otra/ruta/foto.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    private void pausarPerfilDelDueno() {
        Worker worker = workerRepository.findById(workerDelDueno).orElseThrow();
        worker.setStatus(WorkerStatus.PAUSED);
        workerRepository.save(worker);
    }

    private String perfilJson() {
        return """
                {"availableNow": false, "zoneIds": [%d],
                 "primaryTrade": {"tradeId": "%s", "approxRate": 50000, "rateUnit": "PER_JOB"}}
                """.formatted(ZONA, plomeria.getId());
    }

    private String bearer(UUID userId, String role) {
        return "Bearer " + TestTokens.generate(secret, userId, role);
    }


    private String oficioJson() {
        return """
                {"name": "Electricidad", "active": true, "categoryId": "%s"}
                """.formatted(plomeria.getCategory().getId());
    }
}
