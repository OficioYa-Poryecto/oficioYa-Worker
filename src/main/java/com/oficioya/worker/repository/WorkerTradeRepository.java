package com.oficioya.worker.repository;

import com.oficioya.worker.entity.WorkerTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerTradeRepository extends JpaRepository<WorkerTrade, UUID> {

    List<WorkerTrade> findByWorkerId(UUID workerId);

    Optional<WorkerTrade> findByWorkerIdAndPrimaryTrue(UUID workerId);
}