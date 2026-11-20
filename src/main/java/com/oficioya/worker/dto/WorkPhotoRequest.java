package com.oficioya.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// La foto ya está subida; aquí solo se registra su URL. Se acepta una URL http(s) absoluta
// o la ruta relativa del servicio de imágenes (/api/v1/images/{id}), que no depende del dominio del despliegue.
public record WorkPhotoRequest(
        @NotBlank @Size(max = 500)
        @Pattern(regexp = "^(https?://\\S+|/api/v1/images/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$",
                message = "must be an http(s) URL or an image service path (/api/v1/images/{id})") String url,
        @Size(max = 200) String description) {
}
