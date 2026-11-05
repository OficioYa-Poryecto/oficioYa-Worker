package com.oficioya.worker.dto;

import com.oficioya.worker.model.RateUnit;

import java.math.BigDecimal;

public record WorkerTradeResponse(Long id, BigDecimal approxRate, RateUnit rateUnit, TradeResponse trade) {
}
