package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotNull;

public record AvailabilityNowRequest(@NotNull Boolean availableNow) {
}
