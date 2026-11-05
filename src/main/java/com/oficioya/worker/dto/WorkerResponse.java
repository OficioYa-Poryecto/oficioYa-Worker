package com.oficioya.worker.dto;

import com.oficioya.worker.model.WorkerStatus;

import java.util.List;
import java.util.Set;

// Perfil público del trabajador. No incluye correo ni teléfono: esos datos son de Identity
// y solo los ve la administración del sistema.
public record WorkerResponse(Long id, Long userId, WorkerStatus status, boolean availableNow,
                             Set<Long> zoneIds,
                             WorkerTradeResponse primaryTrade,
                             List<WorkerTradeResponse> secondaryTrades,
                             List<AvailabilitySlotResponse> slots,
                             List<WorkPhotoResponse> photos) {
}
