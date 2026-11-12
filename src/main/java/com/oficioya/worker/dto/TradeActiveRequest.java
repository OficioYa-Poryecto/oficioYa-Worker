package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotNull;

public record TradeActiveRequest(@NotNull Boolean active) {
}