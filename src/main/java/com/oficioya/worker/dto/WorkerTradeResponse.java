package com.oficioya.worker.dto;

import com.oficioya.worker.model.RateUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkerTradeResponse(UUID id, BigDecimal approxRate, RateUnit rateUnit, TradeResponse trade) {
}
