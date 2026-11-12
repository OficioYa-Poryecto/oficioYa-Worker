package com.oficioya.worker.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class WorkPhotoModel {
    private UUID id;
    private String url;
    private String description;
    private LocalDateTime uploadedAt;

    public WorkPhotoModel() {
    }

    public WorkPhotoModel(UUID id, String url, String description, LocalDateTime uploadedAt) {
        this.id = id;
        this.url = url;
        this.description = description;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}