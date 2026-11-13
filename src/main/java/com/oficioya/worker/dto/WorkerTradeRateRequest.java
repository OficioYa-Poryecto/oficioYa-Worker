package com.oficioya.worker.dto;

import com.oficioya.worker.model.RateUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Cambio de tarifa de un oficio ya asociado; el oficio en sí no se cambia
public record WorkerTradeRateRequest(
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal approxRate,
        @NotNull RateUnit rateUnit) {
}
