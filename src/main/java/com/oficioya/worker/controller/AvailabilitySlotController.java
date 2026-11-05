package com.oficioya.worker.controller;

import com.oficioya.worker.dto.AvailabilitySlotRequest;
import com.oficioya.worker.dto.AvailabilitySlotResponse;
import com.oficioya.worker.mapper.AvailabilitySlotDtoMapper;
import com.oficioya.worker.service.AvailabilitySlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workers/{workerId}/availability-slots")
@Tag(name = "Availability Slots", description = "Franjas de disponibilidad del trabajador")
public class AvailabilitySlotController {

    private final AvailabilitySlotService slotService;
    private final AvailabilitySlotDtoMapper slotDtoMapper;

    public AvailabilitySlotController(AvailabilitySlotService slotService, AvailabilitySlotDtoMapper slotDtoMapper) {
        this.slotService = slotService;
        this.slotDtoMapper = slotDtoMapper;
    }

    @GetMapping
    @Operation(summary = "Listar disponibilidad", description = "Devuelve las franjas de disponibilidad del trabajador")
    public ResponseEntity<List<AvailabilitySlotResponse>> findAll(@PathVariable Long workerId) {
        return ResponseEntity.ok(slotDtoMapper.toResponseList(slotService.findAll(workerId)));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @PostMapping
    @Operation(summary = "Crear franja de disponibilidad", description = "Registra una nueva franja para el trabajador")
    public ResponseEntity<AvailabilitySlotResponse> create(@PathVariable Long workerId,
                                                           @Valid @RequestBody AvailabilitySlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slotDtoMapper.toResponse(slotService.create(workerId, slotDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @PutMapping("/{slotId}")
    @Operation(summary = "Actualizar franja", description = "Modifica una franja de disponibilidad existente")
    public ResponseEntity<AvailabilitySlotResponse> update(@PathVariable Long workerId,
                                                           @PathVariable Long slotId,
                                                           @Valid @RequestBody AvailabilitySlotRequest request) {
        return ResponseEntity.ok(
                slotDtoMapper.toResponse(slotService.update(workerId, slotId, slotDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @DeleteMapping("/{slotId}")
    @Operation(summary = "Eliminar franja", description = "Elimina una franja de disponibilidad del trabajador")
    public ResponseEntity<Void> delete(@PathVariable Long workerId, @PathVariable Long slotId) {
        slotService.delete(workerId, slotId);
        return ResponseEntity.noContent().build();
    }
}
