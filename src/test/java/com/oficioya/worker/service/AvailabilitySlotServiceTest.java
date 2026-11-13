package com.oficioya.worker.service;

import com.oficioya.worker.entity.AvailabilitySlot;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.exception.BusinessRuleException;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.AvailabilitySlotMapperImpl;
import com.oficioya.worker.model.AvailabilitySlotModel;
import com.oficioya.worker.model.RecurrenceType;
import com.oficioya.worker.model.WorkerStatus;
import com.oficioya.worker.repository.AvailabilitySlotRepository;
import com.oficioya.worker.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilitySlotServiceTest {

    // 2026-11-02 y 2026-11-09 son lunes; 2026-11-10 es martes
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final LocalDate OTRO_LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 10);
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID OTHER_WORKER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_WORKER_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID OTHER_SLOT_ID = UUID.randomUUID();
    private static final UUID SAVED_SLOT_ID = UUID.randomUUID();

    @Mock private WorkerRepository workerRepository;
    @Mock private AvailabilitySlotRepository slotRepository;

    private AvailabilitySlotService slotService;
    private Worker worker;

    @BeforeEach
    void setUp() {
        slotService = new AvailabilitySlotService(workerRepository, slotRepository, new AvailabilitySlotMapperImpl());
        worker = new Worker();
        worker.setId(WORKER_ID);
        worker.setStatus(WorkerStatus.ACTIVE);
    }

    private AvailabilitySlotModel fixed(LocalDate date, int from, int to) {
        return new AvailabilitySlotModel(null, RecurrenceType.FIXED, date, null, null,
                LocalTime.of(from, 0), LocalTime.of(to, 0));
    }

    private AvailabilitySlot existingMondays(UUID id) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(id);
        slot.setWorker(worker);
        slot.setRecurrenceType(RecurrenceType.RECURRING);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartDate(LUNES);
        slot.setEndDate(LUNES.plusMonths(2));
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(12, 0));
        return slot;
    }

    private void saveReturnsArgument() {
        when(slotRepository.save(any(AvailabilitySlot.class))).thenAnswer(inv -> {
            AvailabilitySlot saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(SAVED_SLOT_ID);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("Crea una franja de fecha fija y descarta día de la semana y fecha final")
    void shouldCreateFixedSlot() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(slotRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of());
        saveReturnsArgument();

        AvailabilitySlotModel model = fixed(MARTES, 8, 12);
        model.setDayOfWeek(DayOfWeek.FRIDAY);
        model.setEndDate(MARTES.plusDays(3));
        AvailabilitySlotModel result = slotService.create(WORKER_ID, model);

        assertEquals(SAVED_SLOT_ID, result.getId());
        assertNull(result.getDayOfWeek());
        assertNull(result.getEndDate());
    }

    @Test
    @DisplayName("Rechaza una franja cuya hora de inicio no es anterior a la de fin")
    void shouldRejectInvertedHours() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));

        assertThrows(IllegalArgumentException.class, () -> slotService.create(WORKER_ID, fixed(MARTES, 12, 8)));
        assertThrows(IllegalArgumentException.class, () -> slotService.create(WORKER_ID, fixed(MARTES, 9, 9)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una franja repetitiva exige día de la semana y fecha final")
    void shouldRequireDayAndEndDateForRecurring() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        AvailabilitySlotModel model = new AvailabilitySlotModel(null, RecurrenceType.RECURRING, LUNES, null,
                null, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThrows(IllegalArgumentException.class, () -> slotService.create(WORKER_ID, model));
        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una franja repetitiva no puede terminar antes de empezar")
    void shouldRejectRecurringEndingBeforeStart() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        AvailabilitySlotModel model = new AvailabilitySlotModel(null, RecurrenceType.RECURRING, LUNES,
                LUNES.minusDays(1), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThrows(IllegalArgumentException.class, () -> slotService.create(WORKER_ID, model));
    }

    @Test
    @DisplayName("Rechaza una franja que se cruza con otra del mismo trabajador")
    void shouldRejectOverlappingSlot() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(slotRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(existingMondays(SLOT_ID)));

        assertThrows(BusinessRuleException.class, () -> slotService.create(WORKER_ID, fixed(OTRO_LUNES, 10, 11)));
        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Acepta una franja en otro día aunque el horario coincida")
    void shouldAcceptSlotOnAnotherDay() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(slotRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(existingMondays(SLOT_ID)));
        saveReturnsArgument();

        assertNotNull(slotService.create(WORKER_ID, fixed(MARTES, 10, 11)).getId());
    }

    @Test
    @DisplayName("Al actualizar una franja no la compara consigo misma")
    void shouldUpdateSlotIgnoringItself() {
        AvailabilitySlot existing = existingMondays(SLOT_ID);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existing));
        when(slotRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(existing));
        saveReturnsArgument();

        AvailabilitySlotModel changes = new AvailabilitySlotModel(null, RecurrenceType.RECURRING, LUNES,
                LUNES.plusMonths(1), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        AvailabilitySlotModel result = slotService.update(WORKER_ID, SLOT_ID, changes);

        assertEquals(LocalTime.of(9, 0), result.getStartTime());
        assertEquals(SLOT_ID, result.getId());
    }

    @Test
    @DisplayName("No modifica una franja de otro trabajador")
    void shouldNotTouchSlotOfAnotherWorker() {
        Worker otro = new Worker();
        otro.setId(OTHER_WORKER_ID);
        AvailabilitySlot ajena = existingMondays(OTHER_SLOT_ID);
        ajena.setWorker(otro);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(slotRepository.findById(OTHER_SLOT_ID)).thenReturn(Optional.of(ajena));

        assertThrows(ResourceNotFoundException.class, () -> slotService.delete(WORKER_ID, OTHER_SLOT_ID));
        verify(slotRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Elimina una franja propia")
    void shouldDeleteOwnSlot() {
        AvailabilitySlot existing = existingMondays(SLOT_ID);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existing));

        slotService.delete(WORKER_ID, SLOT_ID);

        verify(slotRepository).delete(existing);
    }

    @Test
    @DisplayName("Un trabajador pausado no puede modificar su disponibilidad")
    void shouldRejectPausedWorker() {
        worker.setStatus(WorkerStatus.PAUSED);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));

        assertThrows(BusinessRuleException.class, () -> slotService.create(WORKER_ID, fixed(MARTES, 8, 9)));
        verifyNoInteractions(slotRepository);
    }

    @Test
    @DisplayName("Listar la disponibilidad de un trabajador inexistente lanza ResourceNotFoundException")
    void shouldFailListingUnknownWorker() {
        when(workerRepository.findById(UNKNOWN_WORKER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> slotService.findAll(UNKNOWN_WORKER_ID));
    }
}
