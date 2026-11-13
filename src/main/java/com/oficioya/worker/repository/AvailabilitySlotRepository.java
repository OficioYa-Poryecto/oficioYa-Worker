package com.oficioya.worker.repository;

import com.oficioya.worker.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    List<AvailabilitySlot> findByWorkerId(UUID workerId);
}