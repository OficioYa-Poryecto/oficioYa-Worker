package com.oficioya.worker.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.*;
import jakarta.validation.*;


public class WorkerModel {

    private UUID id;
    @NotNull
    private UUID userId;
    private WorkerStatus status;
    private boolean availableNow;
    private Set<Long> zoneIds = new HashSet<>();    
    @NotNull
    @Valid
    private WorkerTradeModel primaryTrade;
    private List<WorkerTradeModel> secondaryTrades = new ArrayList<>();
    private List<AvailabilitySlotModel> slots = new ArrayList<>();
    private List<WorkPhotoModel> photos = new ArrayList<>();

    public WorkerModel() {
        this.zoneIds = new HashSet<>();
        this.slots = new ArrayList<>();
        this.photos = new ArrayList<>();
        this.secondaryTrades = new ArrayList<>();
    }

    public WorkerModel(UUID id, UUID userId, WorkerTradeModel primaryTrade) {
        this();
        this.id = id;
        this.userId = userId;
        this.primaryTrade = primaryTrade;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public boolean isAvailableNow() {
        return availableNow;
    }

    public void setAvailableNow(boolean availableNow) {
        this.availableNow = availableNow;
    }

    public Set<Long> getZoneIds() {
        return zoneIds;
    }

    public void setZoneIds(Set<Long> zoneIds) {
        this.zoneIds = zoneIds;
    }

    public List<AvailabilitySlotModel> getSlots() {
        return slots;
    }

    public void setSlots(List<AvailabilitySlotModel> slots) {
        this.slots = slots;
    }

    public List<WorkPhotoModel> getPhotos() {
        return photos;
    }

    public void setPhotos(List<WorkPhotoModel> photos) {
        this.photos = photos;
    }

    public WorkerTradeModel getPrimaryTrade() {
        return primaryTrade;
    }

    public void setPrimaryTrade(WorkerTradeModel primaryTrade) {
        this.primaryTrade = primaryTrade;
    }

    public List<WorkerTradeModel> getSecondaryTrades() {
        return secondaryTrades;
    }

    public void setSecondaryTrades(List<WorkerTradeModel> secondaryTrades) {
        this.secondaryTrades = secondaryTrades;
    }

    public void addSecondaryTrade(WorkerTradeModel trade) {
        secondaryTrades.add(trade);
    }

    public Optional<WorkerTradeModel> getSecondaryTradeById(UUID workerTradeId) {
        return secondaryTrades.stream().filter(t -> t.getId().equals(workerTradeId)).findFirst();
    }

    public boolean removeSecondaryTradeById(UUID workerTradeId) {
        return secondaryTrades.removeIf(t -> t.getId().equals(workerTradeId));
    }

    public void addSlot(AvailabilitySlotModel slot) {
        slots.add(slot);
    }

    public Optional<AvailabilitySlotModel> getSlotById(UUID slotId) {
        return slots.stream().filter(s -> s.getId().equals(slotId)).findFirst();
    }

    public boolean removeSlotById(UUID slotId) {
        return slots.removeIf(s -> s.getId().equals(slotId));
    }

    public void addPhoto(WorkPhotoModel photo) {
        photos.add(photo);
    }

    public Optional<WorkPhotoModel> getPhotoById(UUID photoId) {
        return photos.stream().filter(p -> p.getId().equals(photoId)).findFirst();
    }

    public boolean removePhotoById(UUID photoId) {
        return photos.removeIf(p -> p.getId().equals(photoId));
    }

    public void addZoneId(Long zoneId) {
        zoneIds.add(zoneId);
    }

    public boolean removeZoneId(Long zoneId) {
        return zoneIds.remove(zoneId);
    }
}