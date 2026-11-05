package com.oficioya.worker.dto;

import java.util.Set;

// Zonas de cobertura (ids de barrios del dominio de ubicaciones)
public record WorkerZonesResponse(Long workerId, Set<Long> zoneIds) {
}
