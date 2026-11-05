package com.oficioya.worker.controller;

import com.oficioya.worker.dto.TradeCategoryRequest;
import com.oficioya.worker.dto.TradeCategoryResponse;
import com.oficioya.worker.mapper.TradeCategoryDtoMapper;
import com.oficioya.worker.service.TradeCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade-categories")
@Tag(name = "Trade Categories", description = "Operaciones relacionadas con categorías de oficios")
public class TradeCategoryController {

    private final TradeCategoryService categoryService;
    private final TradeCategoryDtoMapper categoryDtoMapper;

    public TradeCategoryController(TradeCategoryService categoryService, TradeCategoryDtoMapper categoryDtoMapper) {
        this.categoryService = categoryService;
        this.categoryDtoMapper = categoryDtoMapper;
    }

    @GetMapping
    @Operation(summary = "Listar categorías", description = "Devuelve todas las categorías de oficios")
    public ResponseEntity<List<TradeCategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryDtoMapper.toResponseList(categoryService.findAll()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Crear categoría", description = "Registra una nueva categoría de oficios")
    public ResponseEntity<TradeCategoryResponse> create(@Valid @RequestBody TradeCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryDtoMapper.toResponse(categoryService.create(categoryDtoMapper.toModel(request))));
    }
}
