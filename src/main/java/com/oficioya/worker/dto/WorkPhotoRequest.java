package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// La foto ya está subida (p. ej. a Azure Blob); aquí solo se registra su URL
public record WorkPhotoRequest(
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String url,
        @Size(max = 200) String description) {
}
