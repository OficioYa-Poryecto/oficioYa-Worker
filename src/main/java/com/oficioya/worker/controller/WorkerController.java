package com.oficioya.worker.controller;

import com.oficioya.worker.dto.WorkerCreateRequest;
import com.oficioya.worker.dto.WorkerResponse;
import com.oficioya.worker.dto.WorkerUpdateRequest;
import com.oficioya.worker.dto.WorkerZonesResponse;
import com.oficioya.worker.mapper.WorkerDtoMapper;
import com.oficioya.worker.model.WorkerModel;
import com.oficioya.worker.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/workers")
@Tag(name = "Workers", description = "Operaciones relacionadas con trabajadores")
public class WorkerController {

    private final WorkerService workerService;
    private final WorkerDtoMapper workerDtoMapper;

    public WorkerController(WorkerService workerService, WorkerDtoMapper workerDtoMapper) {
        this.workerService = workerService;
        this.workerDtoMapper = workerDtoMapper;
    }

    // El perfil se crea solo para el usuario del token
    @PreAuthorize("hasRole('TRABAJADOR')")
    @PostMapping
    @Operation(summary = "Crear trabajador", description = "Registra un trabajador nuevo con estado Activo")
    public ResponseEntity<WorkerResponse> create(@Valid @RequestBody WorkerCreateRequest request,
                                                Authentication authentication) {
        WorkerModel model = workerDtoMapper.toModel(request);
        model.setUserId(UUID.fromString(authentication.getName()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerDtoMapper.toResponse(workerService.create(model)));
    }

    @GetMapping
    @Operation(summary = "buscar trabajadores", description = "Devuelve todos los trabajadores registrados segun ciertos parametros")
    public ResponseEntity<List<WorkerResponse>> search(@RequestParam(required = false) UUID tradeId,
                                                       @RequestParam(required = false) Long zoneId,
                                                       @RequestParam(required = false) BigDecimal maxRate,
                                                       @RequestParam(required = false) Boolean availableNow) {
        return ResponseEntity.ok(workerDtoMapper.toResponseList(
                workerService.search(tradeId, zoneId, maxRate, availableNow)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener trabajador por ID", description = "Devuelve un trabajador según su identificador")
    public ResponseEntity<WorkerResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(workerService.findById(id)));
    }

    @PreAuthorize("hasRole('TRABAJADOR')")
    @GetMapping("/me")
    @Operation(summary = "Obtener mi perfil", description = "Devuelve el perfil del trabajador autenticado; 404 si aún no lo ha creado")
    public ResponseEntity<WorkerResponse> findMe(Authentication authentication) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(
                workerService.findByUserId(UUID.fromString(authentication.getName()))));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRABAJADOR') and @workerOwnership.isOwner(#id, authentication))")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar trabajador", description = "Modifica la información de un trabajador que no esté Inactivo")
    public ResponseEntity<WorkerResponse> update(@PathVariable UUID id, @Valid @RequestBody WorkerUpdateRequest request) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(workerService.update(id, workerDtoMapper.toModel(request))));
    }

    @GetMapping("/{id}/zones")
    @Operation(summary = "Listar zonas del trabajador", description = "Devuelve los IDs de las zonas donde trabaja")
    public ResponseEntity<WorkerZonesResponse> findZones(@PathVariable UUID id) {
        return ResponseEntity.ok(new WorkerZonesResponse(id, workerService.findZones(id)));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#id, authentication)")
    @PutMapping("/{id}/zones/{zoneId}")
    @Operation(summary = "Agregar zona al trabajador", description = "Asocia una zona de cobertura al trabajador")
    public ResponseEntity<Void> addZone(@PathVariable UUID id, @PathVariable Long zoneId) {
        workerService.addZone(id, zoneId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#id, authentication)")
    @DeleteMapping("/{id}/zones/{zoneId}")
    @Operation(summary = "Quitar zona del trabajador", description = "Elimina la asociación entre el trabajador y la zona")
    public ResponseEntity<Void> removeZone(@PathVariable UUID id, @PathVariable Long zoneId) {
        workerService.removeZone(id, zoneId);
        return ResponseEntity.noContent().build();
    }
}
