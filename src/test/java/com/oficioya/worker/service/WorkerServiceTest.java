package com.oficioya.worker.service;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.entity.WorkerTrade;
import com.oficioya.worker.exception.BusinessRuleException;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.*;
import com.oficioya.worker.model.*;
import com.oficioya.worker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock private WorkerRepository workerRepository;
    @Mock private WorkerTradeRepository workerTradeRepository;
    @Mock private AvailabilitySlotRepository slotRepository;
    @Mock private WorkPhotoRepository photoRepository;
    @Mock private TradeRepository tradeRepository;

    private WorkerService workerService;
    private Trade plomeria;

    @BeforeEach
    void setUp() {
        TradeMapper tradeMapper = new TradeMapperImpl(new TradeCategoryMapperImpl());
        workerService = new WorkerService(workerRepository, workerTradeRepository, slotRepository, photoRepository,
                tradeRepository, new WorkerMapperImpl(), new WorkerTradeMapperImpl(tradeMapper),
                new AvailabilitySlotMapperImpl(), new WorkPhotoMapperImpl());

        TradeCategory hogar = new TradeCategory();
        hogar.setId(1L);
        hogar.setName("Hogar");
        plomeria = new Trade();
        plomeria.setId(3L);
        plomeria.setName("Plomería");
        plomeria.setActive(true);
        plomeria.setCategory(hogar);
    }

    private Worker worker(Long id, Long userId, WorkerStatus status) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setUserId(userId);
        worker.setStatus(status);
        return worker;
    }

    private WorkerTrade workerTrade(Worker worker, Trade trade, String rate, boolean primary) {
        WorkerTrade workerTrade = new WorkerTrade();
        workerTrade.setId(worker.getId() * 100 + trade.getId());
        workerTrade.setWorker(worker);
        workerTrade.setTrade(trade);
        workerTrade.setApproxRate(new BigDecimal(rate));
        workerTrade.setRateUnit(RateUnit.PER_JOB);
        workerTrade.setPrimary(primary);
        return workerTrade;
    }

    private WorkerModel newProfile(Long userId, Long tradeId) {
        TradeModel trade = new TradeModel();
        trade.setId(tradeId);
        WorkerModel model = new WorkerModel(null, userId,
                new WorkerTradeModel(null, new BigDecimal("50000"), RateUnit.PER_JOB, trade));
        model.setZoneIds(Set.of(5L));
        return model;
    }

    // ---- create ----

    @Test
    @DisplayName("Crea el perfil activo con su oficio principal")
    void shouldCreateWorker() {
        when(workerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(tradeRepository.findById(3L)).thenReturn(Optional.of(plomeria));
        when(workerRepository.save(any(Worker.class))).thenAnswer(inv -> {
            Worker saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        WorkerModel model = newProfile(10L, 3L);
        model.setStatus(WorkerStatus.PAUSED);   // el estado que mande el cliente se ignora
        WorkerModel result = workerService.create(model);

        assertEquals(1L, result.getId());
        assertEquals(WorkerStatus.ACTIVE, result.getStatus());
        assertEquals(Set.of(5L), result.getZoneIds());

        ArgumentCaptor<WorkerTrade> captor = ArgumentCaptor.forClass(WorkerTrade.class);
        verify(workerTradeRepository).save(captor.capture());
        assertTrue(captor.getValue().isPrimary());
        assertSame(plomeria, captor.getValue().getTrade());
        assertEquals(1L, captor.getValue().getWorker().getId());
    }

    @Test
    @DisplayName("No crea un segundo perfil para el mismo usuario")
    void shouldRejectDuplicatedProfile() {
        when(workerRepository.findByUserId(10L)).thenReturn(Optional.of(worker(1L, 10L, WorkerStatus.ACTIVE)));

        assertThrows(BusinessRuleException.class, () -> workerService.create(newProfile(10L, 3L)));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("No crea el perfil si el oficio principal no existe")
    void shouldRejectUnknownPrimaryTrade() {
        when(workerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(tradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerService.create(newProfile(10L, 99L)));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("No crea el perfil con un oficio inactivo")
    void shouldRejectInactivePrimaryTrade() {
        plomeria.setActive(false);
        when(workerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(tradeRepository.findById(3L)).thenReturn(Optional.of(plomeria));

        assertThrows(BusinessRuleException.class, () -> workerService.create(newProfile(10L, 3L)));
        verify(workerRepository, never()).save(any());
    }

    // ---- consultas ----

    @Test
    @DisplayName("findById arma el perfil separando oficio principal y secundarios")
    void shouldBuildFullProfile() {
        Worker worker = worker(1L, 10L, WorkerStatus.ACTIVE);
        Trade costura = new Trade();
        costura.setId(4L);
        costura.setName("Costura");
        costura.setCategory(plomeria.getCategory());
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findByWorkerId(1L)).thenReturn(List.of(
                workerTrade(worker, plomeria, "50000", true), workerTrade(worker, costura, "20000", false)));

        WorkerModel result = workerService.findById(1L);

        assertEquals("Plomería", result.getPrimaryTrade().getTrade().getName());
        assertEquals(1, result.getSecondaryTrades().size());
        assertEquals("Costura", result.getSecondaryTrades().get(0).getTrade().getName());
    }

    @Test
    @DisplayName("findById lanza ResourceNotFoundException si el trabajador no existe")
    void shouldFailWhenWorkerNotFound() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerService.findById(99L));
    }

    @Test
    @DisplayName("La búsqueda filtra por zona, disponibilidad, oficio y tarifa máxima")
    void shouldSearchWithFilters() {
        Worker barato = worker(1L, 10L, WorkerStatus.ACTIVE);
        barato.setAvailableNow(true);
        barato.getZoneIds().add(5L);
        Worker caro = worker(2L, 20L, WorkerStatus.ACTIVE);
        caro.setAvailableNow(true);
        caro.getZoneIds().add(5L);
        Worker otraZona = worker(3L, 30L, WorkerStatus.ACTIVE);
        otraZona.setAvailableNow(true);
        otraZona.getZoneIds().add(8L);
        when(workerRepository.findByStatus(WorkerStatus.ACTIVE)).thenReturn(List.of(barato, caro, otraZona));
        when(workerTradeRepository.findByWorkerId(1L)).thenReturn(List.of(workerTrade(barato, plomeria, "40000", true)));
        when(workerTradeRepository.findByWorkerId(2L)).thenReturn(List.of(workerTrade(caro, plomeria, "90000", true)));

        List<WorkerModel> result = workerService.search(3L, 5L, new BigDecimal("50000"), true);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("La búsqueda solo incluye trabajadores activos")
    void shouldSearchOnlyActiveWorkers() {
        when(workerRepository.findByStatus(WorkerStatus.ACTIVE)).thenReturn(List.of());

        assertTrue(workerService.search(null, null, null, null).isEmpty());
        verify(workerRepository).findByStatus(WorkerStatus.ACTIVE);
        verify(workerRepository, never()).findAll();
    }

    @Test
    @DisplayName("La búsqueda rechaza una tarifa máxima negativa")
    void shouldRejectNegativeMaxRate() {
        assertThrows(IllegalArgumentException.class,
                () -> workerService.search(null, null, new BigDecimal("-1"), null));
        verifyNoInteractions(workerRepository);
    }

    // ---- update ----

    @Test
    @DisplayName("Al pausar el perfil deja de estar disponible ahora")
    void shouldClearAvailableNowWhenPaused() {
        Worker worker = worker(1L, 10L, WorkerStatus.ACTIVE);
        worker.setAvailableNow(true);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        WorkerModel changes = new WorkerModel();
        changes.setStatus(WorkerStatus.PAUSED);
        changes.setAvailableNow(true);
        WorkerModel result = workerService.update(1L, changes);

        assertEquals(WorkerStatus.PAUSED, result.getStatus());
        assertFalse(result.isAvailableNow());
    }

    @Test
    @DisplayName("Marca al trabajador como disponible ahora conservando su estado")
    void shouldUpdateAvailableNow() {
        Worker worker = worker(1L, 10L, WorkerStatus.ACTIVE);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        WorkerModel changes = new WorkerModel();
        changes.setAvailableNow(true);
        WorkerModel result = workerService.update(1L, changes);

        assertEquals(WorkerStatus.ACTIVE, result.getStatus());
        assertTrue(result.isAvailableNow());
    }

    @Test
    @DisplayName("Un perfil pausado solo se puede reactivar")
    void shouldOnlyReactivatePausedWorker() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 10L, WorkerStatus.PAUSED)));

        WorkerModel changes = new WorkerModel();
        changes.setAvailableNow(true);

        assertThrows(BusinessRuleException.class, () -> workerService.update(1L, changes));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactiva un perfil pausado")
    void shouldReactivatePausedWorker() {
        Worker worker = worker(1L, 10L, WorkerStatus.PAUSED);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        WorkerModel changes = new WorkerModel();
        changes.setStatus(WorkerStatus.ACTIVE);
        assertEquals(WorkerStatus.ACTIVE, workerService.update(1L, changes).getStatus());
    }

    // ---- zonas ----

    @Test
    @DisplayName("Agrega una zona de cobertura")
    void shouldAddZone() {
        Worker worker = worker(1L, 10L, WorkerStatus.ACTIVE);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        workerService.addZone(1L, 5L);

        assertTrue(worker.getZoneIds().contains(5L));
        verify(workerRepository).save(worker);
    }

    @Test
    @DisplayName("Un trabajador pausado no puede cambiar sus zonas")
    void shouldNotAddZoneToPausedWorker() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 10L, WorkerStatus.PAUSED)));

        assertThrows(BusinessRuleException.class, () -> workerService.addZone(1L, 5L));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quitar una zona que el trabajador no cubre lanza ResourceNotFoundException")
    void shouldFailRemovingUncoveredZone() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 10L, WorkerStatus.ACTIVE)));

        assertThrows(ResourceNotFoundException.class, () -> workerService.removeZone(1L, 5L));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quita una zona cubierta y lista las restantes")
    void shouldRemoveZone() {
        Worker worker = worker(1L, 10L, WorkerStatus.ACTIVE);
        worker.getZoneIds().addAll(Set.of(5L, 6L));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        workerService.removeZone(1L, 5L);

        assertEquals(Set.of(6L), workerService.findZones(1L));
    }
}
