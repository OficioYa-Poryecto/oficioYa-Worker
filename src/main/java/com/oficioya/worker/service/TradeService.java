package com.oficioya.worker.service;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.TradeMapper;
import com.oficioya.worker.model.TradeModel;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private final TradeRepository tradeRepository;
    private final TradeCategoryRepository categoryRepository;
    private final TradeMapper tradeMapper;

    public TradeService(TradeRepository tradeRepository,
                        TradeCategoryRepository categoryRepository,
                        TradeMapper tradeMapper) {
        this.tradeRepository = tradeRepository;
        this.categoryRepository = categoryRepository;
        this.tradeMapper = tradeMapper;
    }

    public List<TradeModel> findAll(Boolean active) {
        log.debug("Listing trades, active filter: {}", active);
        List<Trade> trades = Boolean.TRUE.equals(active)
                ? tradeRepository.findByActiveTrue()
                : tradeRepository.findAll();
        return tradeMapper.toModelList(trades);
    }

    public TradeModel findById(UUID id) {
        log.debug("Looking for trade with id: {}", id);
        return tradeMapper.toModel(getTradeOrThrow(id));
    }

    public TradeModel create(TradeModel model) {
        Trade entity = tradeMapper.toEntity(model);
        entity.setId(null);
        entity.setActive(true);                       // los oficios nuevos nacen activos
        entity.setCategory(getCategoryOrThrow(model.getCategory().getId()));
        TradeModel saved = tradeMapper.toModel(tradeRepository.save(entity));
        log.info("Trade created with id: {}", saved.getId());
        return saved;
    }

    public TradeModel update(UUID id, TradeModel model) {
        Trade entity = getTradeOrThrow(id);
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());
        entity.setActive(model.isActive());
        entity.setCategory(getCategoryOrThrow(model.getCategory().getId()));
        TradeModel saved = tradeMapper.toModel(tradeRepository.save(entity));
        log.info("Trade updated: {}", id);
        return saved;
    }

    public void deactivate(UUID id) {
        Trade entity = getTradeOrThrow(id);
        entity.setActive(false);
        tradeRepository.save(entity);
        log.info("Trade deactivated: {}", id);
    }

    private Trade getTradeOrThrow(UUID id) {
        return tradeRepository.findById(id).orElseThrow(() -> {
            log.warn("Trade not found: {}", id);
            return new ResourceNotFoundException("Trade not found: " + id);
        });
    }

    private TradeCategory getCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> {
            log.warn("Trade category not found: {}", id);
            return new ResourceNotFoundException("Trade category not found: " + id);
        });
    }
}