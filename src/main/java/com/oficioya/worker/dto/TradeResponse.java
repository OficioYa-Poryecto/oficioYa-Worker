package com.oficioya.worker.dto;

import java.util.UUID;

public record TradeResponse(UUID id, String name, String description, boolean active,
                            TradeCategoryResponse category) {
}
