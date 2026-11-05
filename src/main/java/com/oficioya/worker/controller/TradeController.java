package com.oficioya.worker.controller;

import com.oficioya.worker.dto.TradeRequest;
import com.oficioya.worker.dto.TradeResponse;
import com.oficioya.worker.mapper.TradeDtoMapper;
import com.oficioya.worker.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades", description = "Operaciones relacionadas con oficios")
public class TradeController {

    private final TradeService tradeService;
    private final TradeDtoMapper tradeDtoMapper;

    public TradeController(TradeService tradeService, TradeDtoMapper tradeDtoMapper) {
        this.tradeService = tradeService;
        this.tradeDtoMapper = tradeDtoMapper;
    }

    @GetMapping
    @Operation(summary = "Listar oficios", description = "Devuelve los oficios; permite filtrar por activos con el parámetro active")
    public ResponseEntity<List<TradeResponse>> findAll(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(tradeDtoMapper.toResponseList(tradeService.findAll(active)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener oficio por ID", description = "Devuelve un oficio según su identificador")
    public ResponseEntity<TradeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tradeDtoMapper.toResponse(tradeService.findById(id)));
    }

    // Solo el administrador gestiona el catálogo; las consultas quedan para cualquier usuario autenticado

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Crear oficio", description = "Registra un nuevo oficio")
    public ResponseEntity<TradeResponse> create(@Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tradeDtoMapper.toResponse(tradeService.create(tradeDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar oficio", description = "Modifica los datos de un oficio")
    public ResponseEntity<TradeResponse> update(@PathVariable Long id, @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.ok(tradeDtoMapper.toResponse(tradeService.update(id, tradeDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar oficio", description = "Inactiva el oficio; no se elimina físicamente")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        tradeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
