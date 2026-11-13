package com.oficioya.worker.repository;

import com.oficioya.worker.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    List<Trade> findByNameContainingIgnoreCase(String name);

    List<Trade> findByActiveTrue();
}