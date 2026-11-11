package com.oficioya.worker.dto;

import com.oficioya.worker.model.WorkerStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// Perfil público del trabajador. No incluye correo ni teléfono: esos datos son de Identity
// y solo los ve la administración del sistema.
public record WorkerResponse(UUID id, UUID userId, WorkerStatus status, boolean availableNow,
                             Set<Long> zoneIds,
                             WorkerTradeResponse primaryTrade,
                             List<WorkerTradeResponse> secondaryTrades,
                             List<AvailabilitySlotResponse> slots,
                             List<WorkPhotoResponse> photos) {
}
