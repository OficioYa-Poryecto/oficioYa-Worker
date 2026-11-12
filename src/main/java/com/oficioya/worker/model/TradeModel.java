package com.oficioya.worker.model;

import jakarta.validation.constraints.*;

import java.util.UUID;

public class TradeModel {
    private UUID id;
    @NotBlank
    private String name;
    private String description;
    private boolean active;
    @NotNull
    private TradeCategoryModel category;

    public TradeModel() {
    }

    public TradeModel(UUID id, String name, String description, boolean active, TradeCategoryModel category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TradeCategoryModel getCategory() {
        return category;
    }

    public void setCategory(TradeCategoryModel category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}