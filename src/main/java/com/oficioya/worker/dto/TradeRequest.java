package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// active es opcional: si no se envía se toma true. Al crear, el service siempre lo deja activo.
public record TradeRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 200) String description,
        Boolean active,
        @NotNull @Positive Long categoryId) {
}
