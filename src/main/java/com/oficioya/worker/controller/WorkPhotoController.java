package com.oficioya.worker.controller;

import com.oficioya.worker.dto.WorkPhotoRequest;
import com.oficioya.worker.dto.WorkPhotoResponse;
import com.oficioya.worker.mapper.WorkPhotoDtoMapper;
import com.oficioya.worker.service.WorkPhotoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workers/{workerId}/photos")
@Tag(name = "Work Photos", description = "Fotos de trabajos realizados por el trabajador")
public class WorkPhotoController {

    private final WorkPhotoService photoService;
    private final WorkPhotoDtoMapper photoDtoMapper;

    public WorkPhotoController(WorkPhotoService photoService, WorkPhotoDtoMapper photoDtoMapper) {
        this.photoService = photoService;
        this.photoDtoMapper = photoDtoMapper;
    }

    @GetMapping
    @Operation(summary = "Listar fotos", description = "Devuelve las fotos de trabajos del trabajador")
    public ResponseEntity<List<WorkPhotoResponse>> findAll(@PathVariable Long workerId) {
        return ResponseEntity.ok(photoDtoMapper.toResponseList(photoService.findAll(workerId)));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @PostMapping
    @Operation(summary = "Agregar foto", description = "Registra una foto de un trabajo realizado")
    public ResponseEntity<WorkPhotoResponse> create(@PathVariable Long workerId,
                                                    @Valid @RequestBody WorkPhotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(photoDtoMapper.toResponse(photoService.create(workerId, photoDtoMapper.toModel(request))));
    }

    @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")
    @DeleteMapping("/{photoId}")
    @Operation(summary = "Eliminar foto", description = "Elimina una foto del portafolio del trabajador")
    public ResponseEntity<Void> delete(@PathVariable Long workerId, @PathVariable Long photoId) {
        photoService.delete(workerId, photoId);
        return ResponseEntity.noContent().build();
    }
}
