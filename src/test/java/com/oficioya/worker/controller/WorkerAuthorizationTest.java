package com.oficioya.worker.controller;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.model.WorkerStatus;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.TradeRepository;
import com.oficioya.worker.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El username de @WithMockUser es el id del usuario, igual que el "sub" del JWT
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkerAuthorizationTest {

    private static final String DUENO = "10";
    private static final String OTRO = "20";
    private static final long ZONA = 5L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WorkerRepository workerRepository;

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    TradeCategoryRepository categoryRepository;

    private Long workerDelDueno;
    private Trade plomeria;

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
        worker.setUserId(Long.valueOf(DUENO));
        worker.setStatus(WorkerStatus.ACTIVE);
        workerDelDueno = workerRepository.save(worker).getId();
    }

    @Test
    @WithMockUser(username = DUENO, roles = "TRABAJADOR")
    @DisplayName("El trabajador dueño puede agregar una zona a su perfil")
    void duenoAgregaZona() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/zones/" + ZONA).with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(workerRepository.findById(workerDelDueno).orElseThrow().getZoneIds().contains(ZONA));
    }

    @Test
    @WithMockUser(username = OTRO, roles = "TRABAJADOR")
    @DisplayName("Otro trabajador no puede modificar un perfil ajeno: 403 y sin cambios")
    void otroTrabajadorNoModificaPerfilAjeno() throws Exception {
        mockMvc.perform(put("/api/v1/workers/" + workerDelDueno + "/zones/" + ZONA).with(csrf()))
                .andExpect(status().isForbidden());

        assertFalse(workerRepository.findById(workerDelDueno).orElseThrow().getZoneIds().contains(ZONA));
    }

    @Test
    @WithMockUser(username = DUENO, roles = "CONTRATANTE")
    @DisplayName("Un contratante no puede subir fotos al perfil de un trabajador: 403")
    void contratanteNoSubeFotos() throws Exception {
        mockMvc.perform(post("/api/v1/workers/" + workerDelDueno + "/photos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://fotos.example.com/1.jpg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OTRO, roles = "TRABAJADOR")
    @DisplayName("Un trabajador no puede crear un perfil a nombre de otro usuario: 403")
    void noCreaPerfilParaOtroUsuario() throws Exception {
        mockMvc.perform(post("/api/v1/workers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson(Long.valueOf(DUENO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OTRO, roles = "TRABAJADOR")
    @DisplayName("Un trabajador puede crear su propio perfil: 201")
    void creaSuPropioPerfil() throws Exception {
        mockMvc.perform(post("/api/v1/workers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson(Long.valueOf(OTRO))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = DUENO, roles = "TRABAJADOR")
    @DisplayName("Un trabajador no puede crear oficios en el catálogo: 403")
    void trabajadorNoCreaOficios() throws Exception {
        mockMvc.perform(post("/api/v1/trades").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oficioJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    @DisplayName("El administrador puede crear oficios en el catálogo: 201")
    void adminCreaOficios() throws Exception {
        mockMvc.perform(post("/api/v1/trades").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oficioJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = OTRO, roles = "CONTRATANTE")
    @DisplayName("Un contratante puede consultar el perfil público de un trabajador: 200")
    void contratanteConsultaPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/workers/" + workerDelDueno))
                .andExpect(status().isOk());
    }

    private String perfilJson(Long userId) {
        return """
                {"userId": %d, "availableNow": false,
                 "primaryTrade": {"tradeId": %d, "approxRate": 50000, "rateUnit": "PER_JOB"}}
                """.formatted(userId, plomeria.getId());
    }

    private String oficioJson() {
        return """
                {"name": "Electricidad", "active": true, "categoryId": %d}
                """.formatted(plomeria.getCategory().getId());
    }
}
