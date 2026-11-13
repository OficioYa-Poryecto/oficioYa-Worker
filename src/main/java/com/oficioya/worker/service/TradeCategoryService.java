package com.oficioya.worker.service;

import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.exception.BusinessRuleException;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.TradeCategoryMapper;
import com.oficioya.worker.model.TradeCategoryModel;
import com.oficioya.worker.repository.TradeCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TradeCategoryService {

    private static final Logger log = LoggerFactory.getLogger(TradeCategoryService.class);

    private final TradeCategoryRepository categoryRepository;
    private final TradeCategoryMapper categoryMapper;

    public TradeCategoryService(TradeCategoryRepository categoryRepository,
                                TradeCategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<TradeCategoryModel> findAll() {
        log.debug("Listing trade categories");
        return categoryMapper.toModelList(categoryRepository.findAll());
    }

    public TradeCategoryModel findById(UUID id) {
        log.debug("Looking for trade category with id: {}", id);
        return categoryMapper.toModel(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trade category not found: " + id)));
    }

    public TradeCategoryModel create(TradeCategoryModel model) {
        if (categoryRepository.findByNameIgnoreCase(model.getName()).isPresent()) {
            log.warn("Trade category already exists: {}", model.getName());
            throw new BusinessRuleException("Trade category already exists: " + model.getName());
        }
        model.setId(null);
        TradeCategoryModel saved = categoryMapper.toModel(
                categoryRepository.save(categoryMapper.toEntity(model)));
        log.info("Trade category created with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public TradeCategoryModel update(UUID id, TradeCategoryModel model) {
        TradeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trade category not found: " + id));
        categoryRepository.findByNameIgnoreCase(model.getName())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    log.warn("Trade category already exists: {}", model.getName());
                    throw new BusinessRuleException("Trade category already exists: " + model.getName());
                });
        category.setName(model.getName());
        category.setDetail(model.getDetail());
        log.info("Trade category updated with id: {}", id);
        return categoryMapper.toModel(categoryRepository.save(category));
    }
}