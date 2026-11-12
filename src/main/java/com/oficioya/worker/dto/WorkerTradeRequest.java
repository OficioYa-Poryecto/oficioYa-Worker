package com.oficioya.worker.dto;

import com.oficioya.worker.model.RateUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

// Oficio que ofrece el trabajador con su tarifa aproximada
public record WorkerTradeRequest(
        @NotNull UUID tradeId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal approxRate,
        @NotNull RateUnit rateUnit) {
}
