package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TradeCategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 200) String detail) {
}
