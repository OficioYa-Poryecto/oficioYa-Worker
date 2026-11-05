package com.oficioya.worker.service;

import com.oficioya.worker.entity.WorkPhoto;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.exception.BusinessRuleException;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.WorkPhotoMapperImpl;
import com.oficioya.worker.model.WorkPhotoModel;
import com.oficioya.worker.model.WorkerStatus;
import com.oficioya.worker.repository.WorkPhotoRepository;
import com.oficioya.worker.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkPhotoServiceTest {

    @Mock private WorkerRepository workerRepository;
    @Mock private WorkPhotoRepository photoRepository;

    private WorkPhotoService photoService;
    private Worker worker;

    @BeforeEach
    void setUp() {
        photoService = new WorkPhotoService(workerRepository, photoRepository, new WorkPhotoMapperImpl());
        worker = new Worker();
        worker.setId(1L);
        worker.setStatus(WorkerStatus.ACTIVE);
    }

    private WorkPhoto photo(Long id, Worker owner) {
        WorkPhoto photo = new WorkPhoto();
        photo.setId(id);
        photo.setWorker(owner);
        photo.setUrl("https://fotos.example.com/" + id + ".jpg");
        photo.setUploadedAt(LocalDateTime.now());
        return photo;
    }

    @Test
    @DisplayName("Registra la foto con la fecha de carga del servidor")
    void shouldCreatePhoto() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(photoRepository.save(any(WorkPhoto.class))).thenAnswer(inv -> {
            WorkPhoto saved = inv.getArgument(0);
            saved.setId(30L);
            return saved;
        });
        LocalDateTime fechaDelCliente = LocalDateTime.of(2000, 1, 1, 0, 0);

        WorkPhotoModel result = photoService.create(1L,
                new WorkPhotoModel(99L, "https://fotos.example.com/1.jpg", "Baño", fechaDelCliente));

        ArgumentCaptor<WorkPhoto> captor = ArgumentCaptor.forClass(WorkPhoto.class);
        verify(photoRepository).save(captor.capture());
        assertSame(worker, captor.getValue().getWorker());
        assertEquals(30L, result.getId());
        assertNotEquals(fechaDelCliente, result.getUploadedAt(), "la fecha la pone el servidor");
    }

    @Test
    @DisplayName("Un trabajador pausado no puede subir fotos")
    void shouldRejectPausedWorker() {
        worker.setStatus(WorkerStatus.PAUSED);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(BusinessRuleException.class,
                () -> photoService.create(1L, new WorkPhotoModel(null, "https://x.com/a.jpg", null, null)));
        verifyNoInteractions(photoRepository);
    }

    @Test
    @DisplayName("Lista las fotos del trabajador")
    void shouldListPhotos() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(photoRepository.findByWorkerId(1L)).thenReturn(List.of(photo(1L, worker), photo(2L, worker)));

        assertEquals(2, photoService.findAll(1L).size());
    }

    @Test
    @DisplayName("Listar las fotos de un trabajador inexistente lanza ResourceNotFoundException")
    void shouldFailListingUnknownWorker() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> photoService.findAll(99L));
    }

    @Test
    @DisplayName("Elimina una foto propia")
    void shouldDeleteOwnPhoto() {
        WorkPhoto propia = photo(5L, worker);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(photoRepository.findById(5L)).thenReturn(Optional.of(propia));

        photoService.delete(1L, 5L);

        verify(photoRepository).delete(propia);
    }

    @Test
    @DisplayName("No elimina una foto de otro trabajador")
    void shouldNotDeletePhotoOfAnotherWorker() {
        Worker otro = new Worker();
        otro.setId(2L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(photoRepository.findById(6L)).thenReturn(Optional.of(photo(6L, otro)));

        assertThrows(ResourceNotFoundException.class, () -> photoService.delete(1L, 6L));
        verify(photoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Eliminar una foto inexistente lanza ResourceNotFoundException")
    void shouldFailDeletingUnknownPhoto() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(photoRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> photoService.delete(1L, 77L));
    }
}
