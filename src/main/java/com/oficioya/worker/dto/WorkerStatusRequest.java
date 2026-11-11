package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotNull;
import com.oficioya.worker.model.WorkerStatus;

public record WorkerStatusRequest(@NotNull WorkerStatus status) {
}
