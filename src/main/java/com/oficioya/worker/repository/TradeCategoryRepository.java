package com.oficioya.worker.repository;

import com.oficioya.worker.entity.TradeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TradeCategoryRepository extends JpaRepository<TradeCategory, UUID> {

    Optional<TradeCategory> findByNameIgnoreCase(String name);
}