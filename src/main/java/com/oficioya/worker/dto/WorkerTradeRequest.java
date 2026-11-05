package com.oficioya.worker.dto;

import com.oficioya.worker.model.RateUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Oficio que ofrece el trabajador con su tarifa aproximada
public record WorkerTradeRequest(
        @NotNull @Positive Long tradeId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal approxRate,
        @NotNull RateUnit rateUnit) {
}
