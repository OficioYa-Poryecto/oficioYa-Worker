package com.oficioya.worker.service;

import com.oficioya.worker.entity.*;
import com.oficioya.worker.exception.*;
import com.oficioya.worker.mapper.*;
import com.oficioya.worker.model.*;
import com.oficioya.worker.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WorkPhotoService {

    private static final Logger log = LoggerFactory.getLogger(WorkPhotoService.class);

    private final WorkerRepository workerRepository;
    private final WorkPhotoRepository photoRepository;
    private final WorkPhotoMapper photoMapper;

    public WorkPhotoService(WorkerRepository workerRepository,
                            WorkPhotoRepository photoRepository,
                            WorkPhotoMapper photoMapper) {
        this.workerRepository = workerRepository;
        this.photoRepository = photoRepository;
        this.photoMapper = photoMapper;
    }

    public List<WorkPhotoModel> findAll(UUID workerId) {
        getWorkerOrThrow(workerId);
        log.debug("Listing photos of worker {}", workerId);
        return photoMapper.toModelList(photoRepository.findByWorkerId(workerId));
    }

    @Transactional
    public WorkPhotoModel create(UUID workerId, WorkPhotoModel model) {
        Worker worker = getActiveWorkerOrThrow(workerId);
        WorkPhoto entity = photoMapper.toEntity(model);
        entity.setId(null);
        entity.setWorker(worker);
        entity.setUploadedAt(LocalDateTime.now());
        WorkPhotoModel saved = photoMapper.toModel(photoRepository.save(entity));
        log.info("Photo {} added to worker {}", saved.getId(), workerId);
        return saved;
    }

    @Transactional
    public void delete(UUID workerId, UUID photoId) {
        getActiveWorkerOrThrow(workerId);
        WorkPhoto photo = photoRepository.findById(photoId) 
                .filter(p -> p.getWorker().getId().equals(workerId))
                .orElseThrow(() -> {
                    log.warn("Photo {} not found for worker {}", photoId, workerId);
                    return new ResourceNotFoundException("Photo not found: " + photoId);
                });
        photoRepository.delete(photo);
        log.info("Photo {} deleted", photoId);
    }

    private Worker getWorkerOrThrow(UUID id) {
        return workerRepository.findById(id).orElseThrow(() -> {
            log.warn("Worker not found: {}", id);
            return new ResourceNotFoundException("Worker not found: " + id);
        });
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