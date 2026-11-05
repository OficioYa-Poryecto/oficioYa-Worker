package com.oficioya.worker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.HashSet;
import java.util.Set;

// userId debe ser el del token (lo verifica @PreAuthorize en el controlador)
public record WorkerCreateRequest(
        @NotNull @Positive Long userId,
        boolean availableNow,
        Set<@NotNull @Positive Long> zoneIds,
        @NotNull @Valid WorkerTradeRequest primaryTrade) {

    public WorkerCreateRequest {
        zoneIds = zoneIds == null ? new HashSet<>() : zoneIds;
    }
}
