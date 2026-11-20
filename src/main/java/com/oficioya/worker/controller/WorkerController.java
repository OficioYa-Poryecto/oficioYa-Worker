package com.oficioya.worker.controller;

import com.oficioya.worker.dto.AvailabilityNowRequest;
import com.oficioya.worker.dto.WorkerCreateRequest;
import com.oficioya.worker.dto.WorkerResponse;
import com.oficioya.worker.dto.WorkerStatusRequest;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@RestController
@RequestMapping("/api/v1/workers")
@Tag(name = "Workers", description = "Operaciones relacionadas con trabajadores")
public class WorkerController {

    // Roles que pueden ver un perfil aunque esté pausado
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN", "ROLE_SERVICE");

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
    @Operation(summary = "Obtener trabajador por ID",
            description = "Devuelve un trabajador según su identificador; si está pausado responde 404, salvo para su dueño, ADMIN y SERVICE")
    public ResponseEntity<WorkerResponse> findById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(
                workerService.findVisibleById(id, requesterId(authentication), isPrivileged(authentication))));
    }

    @PreAuthorize("hasRole('TRABAJADOR')")
    @GetMapping("/me")
    @Operation(summary = "Obtener mi perfil", description = "Devuelve el perfil del trabajador autenticado; 404 si aún no lo ha creado")
    public ResponseEntity<WorkerResponse> findMe(Authentication authentication) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(
                workerService.findByUserId(UUID.fromString(authentication.getName()))));
    }

    // Para servicios internos y administración: devuelve el perfil aunque esté pausado
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Obtener perfil por usuario", description = "Devuelve el perfil del usuario indicado aunque esté pausado; 404 si no existe. Solo ADMIN y SERVICE")
    public ResponseEntity<WorkerResponse> findByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(workerService.findByUserId(userId)));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#id, authentication)")
    @PutMapping("/{id}/availability-now")
    @Operation(summary = "Cambiar disponible ahora", description = "El trabajador marca si está disponible en este momento")
    public ResponseEntity<WorkerResponse> setAvailableNow(@PathVariable UUID id,
                                                        @Valid @RequestBody AvailabilityNowRequest request) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(
                workerService.setAvailableNow(id, request.availableNow())));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    @PutMapping("/{id}/status")
    @Operation(summary = "Cambiar estado", description = "Pausa o reactiva al trabajador; solo ADMIN y SERVICE")
    public ResponseEntity<WorkerResponse> changeStatus(@PathVariable UUID id,
                                                    @Valid @RequestBody WorkerStatusRequest request) {
        return ResponseEntity.ok(workerDtoMapper.toResponse(workerService.changeStatus(id, request.status())));
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

    // Id del usuario del token; null si la petición es anónima (consultas públicas) o el nombre no es un UUID
    private static UUID requesterId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isPrivileged(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PRIVILEGED_ROLES::contains);
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#id, authentication)")
    @DeleteMapping("/{id}/zones/{zoneId}")
    @Operation(summary = "Quitar zona del trabajador", description = "Elimina la asociación entre el trabajador y la zona")
    public ResponseEntity<Void> removeZone(@PathVariable UUID id, @PathVariable Long zoneId) {
        workerService.removeZone(id, zoneId);
        return ResponseEntity.noContent().build();
    }
}
