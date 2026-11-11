package com.oficioya.worker.repository;

import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.model.WorkerStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    Optional<Worker> findByUserId(Long userId);

    List<Worker> findByAvailableNowTrue();

    List<Worker> findByStatus(WorkerStatus status);
}