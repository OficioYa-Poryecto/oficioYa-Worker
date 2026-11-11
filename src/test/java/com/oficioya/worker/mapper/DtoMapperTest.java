package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.*;
import com.oficioya.worker.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    private final TradeDtoMapper tradeDtoMapper = new TradeDtoMapperImpl(new TradeCategoryDtoMapperImpl());
    private final WorkerTradeDtoMapper workerTradeDtoMapper = new WorkerTradeDtoMapperImpl(tradeDtoMapper);
    private final WorkerDtoMapper workerDtoMapper = new WorkerDtoMapperImpl(workerTradeDtoMapper,
            new AvailabilitySlotDtoMapperImpl(), new WorkPhotoDtoMapperImpl());

    @Test
    @DisplayName("TradeRequest: categoryId pasa a category.id y active es true si no se envía")
    void shouldMapTradeRequest() {
        UUID categoryId = UUID.randomUUID();
        TradeModel model = tradeDtoMapper.toModel(new TradeRequest("Plomería", null, null, categoryId));

        assertNull(model.getId());
        assertEquals(categoryId, model.getCategory().getId());
        assertTrue(model.isActive());
    }

    @Test
    @DisplayName("WorkerCreateRequest: el oficio principal llega con tradeId y sin estado")
    void shouldMapWorkerCreateRequest() {
        UUID tradeId = UUID.randomUUID();
        WorkerCreateRequest request = new WorkerCreateRequest(10L, true, null,
                new WorkerTradeRequest(tradeId, new BigDecimal("50000"), RateUnit.PER_JOB));

        WorkerModel model = workerDtoMapper.toModel(request);

        assertEquals(10L, model.getUserId());
        assertNull(model.getStatus());
        assertEquals(Set.of(), model.getZoneIds());
        assertEquals(tradeId, model.getPrimaryTrade().getTrade().getId());
        assertEquals(new BigDecimal("50000"), model.getPrimaryTrade().getApproxRate());
    }

    @Test
    @DisplayName("WorkerResponse solo tiene datos profesionales: nada de correo ni teléfono")
    void workerResponseHasNoContactData() {
        assertTrue(Arrays.stream(WorkerResponse.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .noneMatch(name -> name.contains("email") || name.contains("mail") || name.contains("phone")
                        || name.contains("telefono")));
    }

    @Test
    @DisplayName("WorkerModel a WorkerResponse incluye oficios con su categoría")
    void shouldMapWorkerResponse() {
        TradeModel trade = new TradeModel(UUID.randomUUID(), "Plomería", null, true,
                new TradeCategoryModel(UUID.randomUUID(), "Hogar", null));
        UUID workerId = UUID.randomUUID();
        WorkerModel model = new WorkerModel(workerId, 10L,
                new WorkerTradeModel(UUID.randomUUID(), BigDecimal.TEN, RateUnit.PER_HOUR, trade));
        model.setStatus(WorkerStatus.ACTIVE);

        WorkerResponse response = workerDtoMapper.toResponse(model);

        assertEquals(workerId, response.id());
        assertEquals("Hogar", response.primaryTrade().trade().category().name());
        assertTrue(response.secondaryTrades().isEmpty());
    }
}
