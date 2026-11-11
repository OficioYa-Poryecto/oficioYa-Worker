package com.oficioya.worker.security;

import com.oficioya.worker.repository.WorkerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reglas de propiedad que se usan desde @PreAuthorize, por ejemplo:
 * {@code @PreAuthorize("hasRole('TRABAJADOR') and @workerOwnership.isOwner(#workerId, authentication)")}.
 *
 * Supone que el nombre del usuario autenticado (el "sub" del JWT) es el id del usuario,
 * que es lo que guarda Worker.userId.
 */
@Component("workerOwnership")
public class WorkerOwnership {

    private final WorkerRepository workerRepository;

    public WorkerOwnership(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    // true si el perfil de trabajador pertenece al usuario autenticado.
    // Si el trabajador no existe deja pasar, para que el servicio responda 404.
    public boolean isOwner(UUID workerId, Authentication authentication) {
        if (workerId == null || authentication == null) {
            return false;
        }
        return workerRepository.findById(workerId)
                .map(worker -> worker.getUserId().toString().equals(authentication.getName()))
                .orElse(true);
    }
}
