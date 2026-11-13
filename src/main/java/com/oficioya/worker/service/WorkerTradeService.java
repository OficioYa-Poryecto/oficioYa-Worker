package com.oficioya.worker.service;

import com.oficioya.worker.entity.*;
import com.oficioya.worker.exception.*;
import com.oficioya.worker.mapper.*;
import com.oficioya.worker.model.*;
import com.oficioya.worker.repository.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkerTradeService {

    private static final Logger log = LoggerFactory.getLogger(WorkerTradeService.class);

    private final WorkerRepository workerRepository;
    private final WorkerTradeRepository workerTradeRepository;
    private final TradeRepository tradeRepository;
    private final WorkerTradeMapper workerTradeMapper;

    public WorkerTradeService(WorkerRepository workerRepository,
                              WorkerTradeRepository workerTradeRepository,
                              TradeRepository tradeRepository,
                              WorkerTradeMapper workerTradeMapper) {
        this.workerRepository = workerRepository;
        this.workerTradeRepository = workerTradeRepository;
        this.tradeRepository = tradeRepository;
        this.workerTradeMapper = workerTradeMapper;
    }

    @Transactional
    public WorkerTradeModel addSecondary(UUID workerId, WorkerTradeModel model) {
        Worker worker = getActiveWorkerOrThrow(workerId);
        Trade trade = tradeRepository.findById(model.getTrade().getId()).orElseThrow(() -> {
            log.warn("Trade not found: {}", model.getTrade().getId());
            return new ResourceNotFoundException("Trade not found: " + model.getTrade().getId());
        });
        if (!trade.isActive()) {
            throw new BusinessRuleException("Trade is not active: " + trade.getId());
        }
        boolean alreadyOffered = workerTradeRepository.findByWorkerId(workerId).stream()
                .anyMatch(wt -> wt.getTrade().getId().equals(trade.getId()));
        if (alreadyOffered) {
            log.warn("Worker {} already offers trade {}", workerId, trade.getId());
            throw new BusinessRuleException("The worker already offers this trade: " + trade.getId());
        }

        WorkerTrade entity = workerTradeMapper.toEntity(model);
        entity.setId(null);
        entity.setWorker(worker);
        entity.setTrade(trade);
        entity.setPrimary(false);
        WorkerTradeModel saved = workerTradeMapper.toModel(workerTradeRepository.save(entity));
        log.info("Secondary trade {} added to worker {}", trade.getId(), workerId);
        return saved;
    }

    @Transactional
    public WorkerTradeModel update(UUID workerId, UUID workerTradeId, WorkerTradeModel model) {
        getActiveWorkerOrThrow(workerId);
        WorkerTrade entity = getWorkerTradeOrThrow(workerId, workerTradeId);
        entity.setApproxRate(model.getApproxRate());
        entity.setRateUnit(model.getRateUnit());
        WorkerTradeModel saved = workerTradeMapper.toModel(workerTradeRepository.save(entity));
        log.info("Rate updated for worker trade {}", workerTradeId);
        return saved;
    }

    @Transactional
    public WorkerTradeModel changePrimary(UUID workerId, UUID workerTradeId) {
        getActiveWorkerOrThrow(workerId);
        WorkerTrade newPrimary = getWorkerTradeOrThrow(workerId, workerTradeId);
        if (!newPrimary.isPrimary()) {
            workerTradeRepository.findByWorkerIdAndPrimaryTrue(workerId).ifPresent(current -> {
                current.setPrimary(false);
                workerTradeRepository.save(current);
            });
            newPrimary.setPrimary(true);
            workerTradeRepository.save(newPrimary);
            log.info("Worker {} primary trade changed to {}", workerId, workerTradeId);
        }
        return workerTradeMapper.toModel(newPrimary);
    }

    @Transactional
    public void deleteSecondary(UUID workerId, UUID workerTradeId) {
        getActiveWorkerOrThrow(workerId);
        WorkerTrade entity = getWorkerTradeOrThrow(workerId, workerTradeId);
        if (entity.isPrimary()) {
            log.warn("Attempt to remove primary trade {} of worker {}", workerTradeId, workerId);
            throw new BusinessRuleException("The primary trade cannot be removed; change the primary trade first");
        }
        workerTradeRepository.delete(entity);
        log.info("Trade {} removed from worker {}", workerTradeId, workerId);
    }

    private Worker getActiveWorkerOrThrow(UUID id) {
        Worker worker = workerRepository.findById(id).orElseThrow(() -> {
            log.warn("Worker not found: {}", id);
            return new ResourceNotFoundException("Worker not found: " + id);
        });
        if (worker.getStatus() == WorkerStatus.PAUSED) {
            log.warn("Worker {} is paused and cannot be modified", id);
            throw new BusinessRuleException("The worker is paused and cannot be modified: " + id);
        }
        return worker;
    }

    private WorkerTrade getWorkerTradeOrThrow(UUID workerId, UUID workerTradeId) {
        return workerTradeRepository.findById(workerTradeId)
                .filter(wt -> wt.getWorker().getId().equals(workerId))
                .orElseThrow(() -> {
                    log.warn("Worker trade {} not found for worker {}", workerTradeId, workerId);
                    return new ResourceNotFoundException("Worker trade not found: " + workerTradeId);
                });
    }
}