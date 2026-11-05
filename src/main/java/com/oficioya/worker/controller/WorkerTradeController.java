package com.oficioya.worker.controller;

import com.oficioya.worker.dto.WorkerTradeRateRequest;
import com.oficioya.worker.dto.WorkerTradeRequest;
import com.oficioya.worker.dto.WorkerTradeResponse;
import com.oficioya.worker.mapper.WorkerTradeDtoMapper;
import com.oficioya.worker.service.WorkerTradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workers/{workerId}")
@Tag(name = "Worker Trades", description = "Oficios principal y secundarios de un trabajador")
public class WorkerTradeController {

    private final WorkerTradeService workerTradeService;
    private final WorkerTradeDtoMapper workerTradeDtoMapper;

    public WorkerTradeController(WorkerTradeService workerTradeService, WorkerTradeDtoMapper workerTradeDtoMapper) {
        this.workerTradeService = workerTradeService;
        this.workerTradeDtoMapper = workerTradeDtoMapper;
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @PostMapping("/trades")
    @Operation(summary = "Agregar oficio secundario", description = "Asocia un oficio secundario al trabajador")
    public ResponseEntity<WorkerTradeResponse> addSecondary(@PathVariable Long workerId,
                                                            @Valid @RequestBody WorkerTradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workerTradeDtoMapper.toResponse(
                workerTradeService.addSecondary(workerId, workerTradeDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @PutMapping("/trades/{workerTradeId}")
    @Operation(summary = "Actualizar tarifa del oficio", description = "Modifica la tarifa de un oficio asociado al trabajador")
    public ResponseEntity<WorkerTradeResponse> update(@PathVariable Long workerId,
                                                      @PathVariable Long workerTradeId,
                                                      @Valid @RequestBody WorkerTradeRateRequest request) {
        return ResponseEntity.ok(workerTradeDtoMapper.toResponse(
                workerTradeService.update(workerId, workerTradeId, workerTradeDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @DeleteMapping("/trades/{workerTradeId}")
    @Operation(summary = "Eliminar oficio secundario", description = "Quita un oficio secundario del trabajador")
    public ResponseEntity<Void> deleteSecondary(@PathVariable Long workerId,
                                                @PathVariable Long workerTradeId) {
        workerTradeService.deleteSecondary(workerId, workerTradeId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @PutMapping("/primary-trade/{workerTradeId}")
    @Operation(summary = "Cambiar oficio principal", description = "Define cuál de los oficios del trabajador es el principal")
    public ResponseEntity<WorkerTradeResponse> changePrimary(@PathVariable Long workerId,
                                                             @PathVariable Long workerTradeId) {
        return ResponseEntity.ok(workerTradeDtoMapper.toResponse(workerTradeService.changePrimary(workerId, workerTradeId)));
    }
}
