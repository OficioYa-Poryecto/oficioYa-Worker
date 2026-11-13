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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkPhotoServiceTest {

    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID OTHER_WORKER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_WORKER_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final UUID OTHER_PHOTO_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_PHOTO_ID = UUID.randomUUID();
    private static final UUID SAVED_PHOTO_ID = UUID.randomUUID();

    @Mock private WorkerRepository workerRepository;
    @Mock private WorkPhotoRepository photoRepository;

    private WorkPhotoService photoService;
    private Worker worker;

    @BeforeEach
    void setUp() {
        photoService = new WorkPhotoService(workerRepository, photoRepository, new WorkPhotoMapperImpl());
        worker = new Worker();
        worker.setId(WORKER_ID);
        worker.setStatus(WorkerStatus.ACTIVE);
    }

    private WorkPhoto photo(UUID id, Worker owner) {
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
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(photoRepository.save(any(WorkPhoto.class))).thenAnswer(inv -> {
            WorkPhoto saved = inv.getArgument(0);
            saved.setId(SAVED_PHOTO_ID);
            return saved;
        });
        LocalDateTime fechaDelCliente = LocalDateTime.of(2000, 1, 1, 0, 0);

        WorkPhotoModel result = photoService.create(WORKER_ID,
                new WorkPhotoModel(UNKNOWN_PHOTO_ID, "https://fotos.example.com/1.jpg", "Baño", fechaDelCliente));

        ArgumentCaptor<WorkPhoto> captor = ArgumentCaptor.forClass(WorkPhoto.class);
        verify(photoRepository).save(captor.capture());
        assertSame(worker, captor.getValue().getWorker());
        assertEquals(SAVED_PHOTO_ID, result.getId());
        assertNotEquals(fechaDelCliente, result.getUploadedAt(), "la fecha la pone el servidor");
    }

    @Test
    @DisplayName("Un trabajador pausado no puede subir fotos")
    void shouldRejectPausedWorker() {
        worker.setStatus(WorkerStatus.PAUSED);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));

        assertThrows(BusinessRuleException.class,
                () -> photoService.create(WORKER_ID, new WorkPhotoModel(null, "https://x.com/a.jpg", null, null)));
        verifyNoInteractions(photoRepository);
    }

    @Test
    @DisplayName("Lista las fotos del trabajador")
    void shouldListPhotos() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(photoRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(photo(PHOTO_ID, worker), photo(OTHER_PHOTO_ID, worker)));

        assertEquals(2, photoService.findAll(WORKER_ID).size());
    }

    @Test
    @DisplayName("Listar las fotos de un trabajador inexistente lanza ResourceNotFoundException")
    void shouldFailListingUnknownWorker() {
        when(workerRepository.findById(UNKNOWN_WORKER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> photoService.findAll(UNKNOWN_WORKER_ID));
    }

    @Test
    @DisplayName("Elimina una foto propia")
    void shouldDeleteOwnPhoto() {
        WorkPhoto propia = photo(PHOTO_ID, worker);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(propia));

        photoService.delete(WORKER_ID, PHOTO_ID);

        verify(photoRepository).delete(propia);
    }

    @Test
    @DisplayName("No elimina una foto de otro trabajador")
    void shouldNotDeletePhotoOfAnotherWorker() {
        Worker otro = new Worker();
        otro.setId(OTHER_WORKER_ID);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(photoRepository.findById(OTHER_PHOTO_ID)).thenReturn(Optional.of(photo(OTHER_PHOTO_ID, otro)));

        assertThrows(ResourceNotFoundException.class, () -> photoService.delete(WORKER_ID, OTHER_PHOTO_ID));
        verify(photoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Eliminar una foto inexistente lanza ResourceNotFoundException")
    void shouldFailDeletingUnknownPhoto() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(photoRepository.findById(UNKNOWN_PHOTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> photoService.delete(WORKER_ID, UNKNOWN_PHOTO_ID));
    }
}
