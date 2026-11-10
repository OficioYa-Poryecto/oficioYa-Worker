package com.oficioya.worker.service;

import com.oficioya.worker.entity.*;
import com.oficioya.worker.exception.*;
import com.oficioya.worker.mapper.*;
import com.oficioya.worker.model.*;
import com.oficioya.worker.repository.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class AvailabilitySlotService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilitySlotService.class);

    private final WorkerRepository workerRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final AvailabilitySlotMapper slotMapper;

    public AvailabilitySlotService(WorkerRepository workerRepository,
                                   AvailabilitySlotRepository slotRepository,
                                   AvailabilitySlotMapper slotMapper) {
        this.workerRepository = workerRepository;
        this.slotRepository = slotRepository;
        this.slotMapper = slotMapper;
    }

    public List<AvailabilitySlotModel> findAll(Long workerId) {
        getWorkerOrThrow(workerId);
        log.debug("Listing availability slots of worker {}", workerId);
        return slotMapper.toModelList(slotRepository.findByWorkerId(workerId));
    }

    @Transactional
    public AvailabilitySlotModel create(Long workerId, AvailabilitySlotModel model) {
        Worker worker = getActiveWorkerOrThrow(workerId);
        AvailabilitySlot entity = slotMapper.toEntity(model);
        entity.setId(null);
        entity.setWorker(worker);
        normalizeAndValidate(entity);
        checkOverlap(entity, null);
        AvailabilitySlotModel saved = slotMapper.toModel(slotRepository.save(entity));
        log.info("Availability slot {} created for worker {}", saved.getId(), workerId);
        return saved;
    }

    @Transactional
    public AvailabilitySlotModel update(Long workerId, Long slotId, AvailabilitySlotModel model) {
        getActiveWorkerOrThrow(workerId);
        AvailabilitySlot entity = getSlotOrThrow(workerId, slotId);
        entity.setRecurrenceType(model.getRecurrenceType());
        entity.setDayOfWeek(model.getDayOfWeek());
        entity.setStartDate(model.getStartDate());
        entity.setEndDate(model.getEndDate());
        entity.setStartTime(model.getStartTime());
        entity.setEndTime(model.getEndTime());
        normalizeAndValidate(entity);
        checkOverlap(entity, slotId);
        AvailabilitySlotModel saved = slotMapper.toModel(slotRepository.save(entity));
        log.info("Availability slot {} updated", slotId);
        return saved;
    }

    @Transactional
    public void delete(Long workerId, Long slotId) {
        getActiveWorkerOrThrow(workerId);
        slotRepository.delete(getSlotOrThrow(workerId, slotId));
        log.info("Availability slot {} deleted", slotId);
    }

    private void normalizeAndValidate(AvailabilitySlot slot) {
        if (!slot.getStartTime().isBefore(slot.getEndTime())) {
            throw new IllegalArgumentException("The start time must be before the end time");
        }
        if (slot.getRecurrenceType() == RecurrenceType.FIXED) {
            slot.setDayOfWeek(null);
            slot.setEndDate(null);
        } else {
            if (slot.getDayOfWeek() == null || slot.getEndDate() == null) {
                throw new IllegalArgumentException("A recurring slot requires day of week and end date");
            }
            if (slot.getEndDate().isBefore(slot.getStartDate())) {
                throw new IllegalArgumentException("The end date cannot be before the start date");
            }
        }
    }

    private void checkOverlap(AvailabilitySlot candidate, Long ignoredId) {
        for (AvailabilitySlot other : slotRepository.findByWorkerId(candidate.getWorker().getId())) {
            if (other.getId().equals(ignoredId)) {
                continue;
            }
            if (overlaps(candidate, other)) {
                log.warn("Slot overlaps with slot {}", other.getId());
                throw new BusinessRuleException("The slot overlaps with an existing slot: " + other.getId());
            }
        }
    }

    private boolean overlaps(AvailabilitySlot a, AvailabilitySlot b) {
        return weekDay(a).equals(weekDay(b))
                && !a.getStartDate().isAfter(lastDate(b))
                && !b.getStartDate().isAfter(lastDate(a))
                && a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
    }

    private DayOfWeek weekDay(AvailabilitySlot slot) {
        return slot.getRecurrenceType() == RecurrenceType.FIXED
                ? slot.getStartDate().getDayOfWeek()
                : slot.getDayOfWeek();
    }

    private LocalDate lastDate(AvailabilitySlot slot) {
        return slot.getRecurrenceType() == RecurrenceType.FIXED ? slot.getStartDate() : slot.getEndDate();
    }

    private Worker getWorkerOrThrow(Long id) {
        return workerRepository.findById(id).orElseThrow(() -> {
            log.warn("Worker not found: {}", id);
            return new ResourceNotFoundException("Worker not found: " + id);
        });
    }

    private Worker getActiveWorkerOrThrow(Long id) {
        Worker worker = getWorkerOrThrow(id);
        if (worker.getStatus() == WorkerStatus.PAUSED) {
            log.warn("Worker {} is paused and cannot be modified", id);
            throw new BusinessRuleException("The worker is paused and cannot be modified: " + id);
        }
        return worker;
    }

    private AvailabilitySlot getSlotOrThrow(Long workerId, Long slotId) {
        return slotRepository.findById(slotId)
                .filter(s -> s.getWorker().getId().equals(workerId))
                .orElseThrow(() -> {
                    log.warn("Availability slot {} not found for worker {}", slotId, workerId);
                    return new ResourceNotFoundException("Availability slot not found: " + slotId);
                });
    }
}