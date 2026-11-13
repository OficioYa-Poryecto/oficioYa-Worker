package com.oficioya.worker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.HashSet;
import java.util.Set;

// El userId no viaja en el cuerpo: se toma del token
public record WorkerCreateRequest(
        boolean availableNow,
        Set<@NotNull @Positive Long> zoneIds,
        @NotNull @Valid WorkerTradeRequest primaryTrade) {

    public WorkerCreateRequest {
        zoneIds = zoneIds == null ? new HashSet<>() : zoneIds;
    }
}
