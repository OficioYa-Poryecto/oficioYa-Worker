package com.oficioya.worker.dto;

import com.oficioya.worker.model.WorkerStatus;

// status es opcional: si no se envía se conserva el actual
public record WorkerUpdateRequest(WorkerStatus status, boolean availableNow) {
}
