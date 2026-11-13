package com.oficioya.worker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkPhotoResponse(UUID id, String url, String description, LocalDateTime uploadedAt) {
}
