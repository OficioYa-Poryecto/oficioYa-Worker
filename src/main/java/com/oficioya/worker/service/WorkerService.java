package com.oficioya.worker.service;

import com.oficioya.worker.entity.*;
import com.oficioya.worker.exception.*;
import com.oficioya.worker.mapper.*;
import com.oficioya.worker.model.*;
import com.oficioya.worker.repository.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;

@Service
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final WorkerRepository workerRepository;
    private final WorkerTradeRepository workerTradeRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final WorkPhotoRepository photoRepository;
    private final TradeRepository tradeRepository;
    private final WorkerMapper workerMapper;
    private final WorkerTradeMapper workerTradeMapper;
    private final AvailabilitySlotMapper slotMapper;
    private final WorkPhotoMapper photoMapper;

    public WorkerService(WorkerRepository workerRepository,
                         WorkerTradeRepository workerTradeRepository,
                         AvailabilitySlotRepository slotRepository,
                         WorkPhotoRepository photoRepository,
                         TradeRepository tradeRepository,
                         WorkerMapper workerMapper,
                         WorkerTradeMapper workerTradeMapper,
                         AvailabilitySlotMapper slotMapper,
                         WorkPhotoMapper photoMapper) {
        this.workerRepository = workerRepository;
        this.workerTradeRepository = workerTradeRepository;
        this.slotRepository = slotRepository;
        this.photoRepository = photoRepository;
        this.tradeRepository = tradeRepository;
        this.workerMapper = workerMapper;
        this.workerTradeMapper = workerTradeMapper;
        this.slotMapper = slotMapper;
        this.photoMapper = photoMapper;
    }

    public List<WorkerModel> search(UUID tradeId, Long zoneId, BigDecimal maxRate, Boolean availableNow) {
        log.info("Worker search: tradeId={}, zoneId={}, maxRate={}, availableNow={}",
                tradeId, zoneId, maxRate, availableNow);
        if (maxRate != null && maxRate.signum() < 0) {
            throw new IllegalArgumentException("The maximum rate cannot be negative");
        }
        List<WorkerModel> result = workerRepository.findByStatus(WorkerStatus.ACTIVE).stream()
                .filter(w -> availableNow == null || w.isAvailableNow() == availableNow)
                .filter(w -> zoneId == null || w.getZoneIds().contains(zoneId))
                .filter(w -> matchesTrade(w, tradeId, maxRate))
                .map(this::buildModel)
                .toList();
        log.info("Worker search returned {} workers", result.size());
        return result;
    }

    private boolean matchesTrade(Worker worker, UUID tradeId, BigDecimal maxRate) {
        if (tradeId == null && maxRate == null) {
            return true;
        }
        return workerTradeRepository.findByWorkerId(worker.getId()).stream()
                .anyMatch(wt -> (tradeId == null || wt.getTrade().getId().equals(tradeId))
                        && (maxRate == null || wt.getApproxRate().compareTo(maxRate) <= 0));
    }

    public WorkerModel findById(UUID id) {
        log.debug("Looking for worker with id: {}", id);
        return buildModel(getWorkerOrThrow(id));
    }

    @Transactional
    public WorkerModel create(WorkerModel model) {
        if (workerRepository.findByUserId(model.getUserId()).isPresent()) {
            log.warn("User {} already has a worker profile", model.getUserId());
            throw new BusinessRuleException("The user already has a worker profile: " + model.getUserId());
        }
        Trade trade = getTradeOrThrow(model.getPrimaryTrade().getTrade().getId());
        if (!trade.isActive()) {
            throw new BusinessRuleException("Trade is not active: " + trade.getId());
        }

        Worker worker = workerMapper.toEntity(model);
        worker.setId(null);
        worker.setStatus(WorkerStatus.ACTIVE);          // siempre nace activo
        Worker savedWorker = workerRepository.save(worker);

        WorkerTrade primary = workerTradeMapper.toEntity(model.getPrimaryTrade());
        primary.setId(null);
        primary.setWorker(savedWorker);
        primary.setTrade(trade);
        primary.setPrimary(true);
        workerTradeRepository.save(primary);

        log.info("Worker created with id: {}", savedWorker.getId());
        return buildModel(savedWorker);
    }

    @Transactional
    public WorkerModel update(UUID id, WorkerModel model) {
        Worker worker = getWorkerOrThrow(id);
        WorkerStatus newStatus = model.getStatus() != null ? model.getStatus() : worker.getStatus();

        if (worker.getStatus() == WorkerStatus.PAUSED && newStatus != WorkerStatus.ACTIVE) {
            log.warn("Worker {} is paused and cannot be modified", id);
            throw new BusinessRuleException("The worker is paused; it can only be reactivated");
        }

        worker.setStatus(newStatus);
        worker.setAvailableNow(newStatus == WorkerStatus.PAUSED ? false : model.isAvailableNow());
        Worker saved = workerRepository.save(worker);

        log.info("Worker updated: {} (status {})", id, newStatus);
        return buildModel(saved);
    }

    // Arma el modelo completo: el mapper copia los campos planos y aquí se cargan las listas
    private WorkerModel buildModel(Worker worker) {
        WorkerModel model = workerMapper.toModel(worker);

        for (WorkerTrade workerTrade : workerTradeRepository.findByWorkerId(worker.getId())) {
            WorkerTradeModel tradeModel = workerTradeMapper.toModel(workerTrade);
            if (workerTrade.isPrimary()) {
                model.setPrimaryTrade(tradeModel);
            } else {
                model.addSecondaryTrade(tradeModel);
            }
        }
        model.setSlots(slotMapper.toModelList(slotRepository.findByWorkerId(worker.getId())));
        model.setPhotos(photoMapper.toModelList(photoRepository.findByWorkerId(worker.getId())));
        return model;
    }

    private Worker getWorkerOrThrow(UUID id) {
        return workerRepository.findById(id).orElseThrow(() -> {
            log.warn("Worker not found: {}", id);
            return new ResourceNotFoundException("Worker not found: " + id);
        });
    }

    private Trade getTradeOrThrow(UUID id) {
        return tradeRepository.findById(id).orElseThrow(() -> {
            log.warn("Trade not found: {}", id);
            return new ResourceNotFoundException("Trade not found: " + id);
        });
    }

    public Set<Long> findZones(UUID workerId) {
        return new HashSet<>(getWorkerOrThrow(workerId).getZoneIds());
    }

    @Transactional
    public void addZone(UUID workerId, Long zoneId) {
        Worker worker = getActiveWorkerOrThrow(workerId);
        worker.getZoneIds().add(zoneId);
        workerRepository.save(worker);
        log.info("Zone {} added to worker {}", zoneId, workerId);
    }

    @Transactional
    public void removeZone(UUID workerId, Long zoneId) {
        Worker worker = getActiveWorkerOrThrow(workerId);
        if (!worker.getZoneIds().remove(zoneId)) {
            log.warn("Worker {} does not cover zone {}", workerId, zoneId);
            throw new ResourceNotFoundException("The worker does not cover zone: " + zoneId);
        }
        workerRepository.save(worker);
        log.info("Zone {} removed from worker {}", zoneId, workerId);
    }

    private Worker getActiveWorkerOrThrow(UUID id) {
        Worker worker = getWorkerOrThrow(id);
        if (worker.getStatus() == WorkerStatus.PAUSED) {
            log.warn("Worker {} is paused and cannot be modified", id);
            throw new BusinessRuleException("The worker is paused and cannot be modified: " + id);
        }
        return worker;
    }
}