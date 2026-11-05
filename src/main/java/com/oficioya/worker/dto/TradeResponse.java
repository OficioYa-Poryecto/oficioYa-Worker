package com.oficioya.worker.dto;

public record TradeResponse(Long id, String name, String description, boolean active,
                            TradeCategoryResponse category) {
}
