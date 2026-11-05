package com.oficioya.worker.dto;

import java.time.LocalDateTime;

public record WorkPhotoResponse(Long id, String url, String description, LocalDateTime uploadedAt) {
}
