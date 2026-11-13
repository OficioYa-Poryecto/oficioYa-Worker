package com.oficioya.worker.dto;

import java.util.Set;
import java.util.UUID;

// Zonas de cobertura (ids de barrios del dominio de ubicaciones)
public record WorkerZonesResponse(UUID workerId, Set<Long> zoneIds) {
}
