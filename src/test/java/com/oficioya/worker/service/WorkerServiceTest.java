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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_USER_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID OTHER_WORKER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_WORKER_ID = UUID.randomUUID();
    private static final UUID TRADE_ID = UUID.randomUUID();
    private static final UUID OTHER_TRADE_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_TRADE_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PRIMARY_WORKER_TRADE_ID = UUID.randomUUID();
    private static final UUID SECONDARY_WORKER_TRADE_ID = UUID.randomUUID();

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
        hogar.setId(CATEGORY_ID);
        hogar.setName("Hogar");
        plomeria = new Trade();
        plomeria.setId(TRADE_ID);
        plomeria.setName("Plomería");
        plomeria.setActive(true);
        plomeria.setCategory(hogar);
    }

    private Worker worker(UUID id, UUID userId, WorkerStatus status) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setUserId(userId);
        worker.setStatus(status);
        return worker;
    }

    private WorkerTrade workerTrade(Worker worker, Trade trade, String rate, boolean primary) {
        WorkerTrade workerTrade = new WorkerTrade();
        workerTrade.setId(primary ? PRIMARY_WORKER_TRADE_ID : SECONDARY_WORKER_TRADE_ID);
        workerTrade.setWorker(worker);
        workerTrade.setTrade(trade);
        workerTrade.setApproxRate(new BigDecimal(rate));
        workerTrade.setRateUnit(RateUnit.PER_JOB);
        workerTrade.setPrimary(primary);
        return workerTrade;
    }

    private WorkerModel newProfile(UUID userId, UUID tradeId) {
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
        when(workerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(tradeRepository.findById(TRADE_ID)).thenReturn(Optional.of(plomeria));
        when(workerRepository.save(any(Worker.class))).thenAnswer(inv -> {
            Worker saved = inv.getArgument(0);
            saved.setId(WORKER_ID);
            return saved;
        });

        WorkerModel model = newProfile(USER_ID, TRADE_ID);
        model.setStatus(WorkerStatus.PAUSED);   // el estado que mande el cliente se ignora
        WorkerModel result = workerService.create(model);

        assertEquals(WORKER_ID, result.getId());
        assertEquals(WorkerStatus.ACTIVE, result.getStatus());
        assertEquals(Set.of(5L), result.getZoneIds());

        ArgumentCaptor<WorkerTrade> captor = ArgumentCaptor.forClass(WorkerTrade.class);
        verify(workerTradeRepository).save(captor.capture());
        assertTrue(captor.getValue().isPrimary());
        assertSame(plomeria, captor.getValue().getTrade());
        assertEquals(WORKER_ID, captor.getValue().getWorker().getId());
    }

    @Test
    @DisplayName("No crea un segundo perfil para el mismo usuario")
    void shouldRejectDuplicatedProfile() {
        when(workerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE)));

        assertThrows(BusinessRuleException.class, () -> workerService.create(newProfile(USER_ID, TRADE_ID)));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("No crea el perfil si el oficio principal no existe")
    void shouldRejectUnknownPrimaryTrade() {
        when(workerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(tradeRepository.findById(UNKNOWN_TRADE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerService.create(newProfile(USER_ID, UNKNOWN_TRADE_ID)));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("No crea el perfil con un oficio inactivo")
    void shouldRejectInactivePrimaryTrade() {
        plomeria.setActive(false);
        when(workerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(tradeRepository.findById(TRADE_ID)).thenReturn(Optional.of(plomeria));

        assertThrows(BusinessRuleException.class, () -> workerService.create(newProfile(USER_ID, TRADE_ID)));
        verify(workerRepository, never()).save(any());
    }

    // ---- consultas ----

    @Test
    @DisplayName("findById arma el perfil separando oficio principal y secundarios")
    void shouldBuildFullProfile() {
        Worker worker = worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE);
        Trade costura = new Trade();
        costura.setId(OTHER_TRADE_ID);
        costura.setName("Costura");
        costura.setCategory(plomeria.getCategory());
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(
                workerTrade(worker, plomeria, "50000", true), workerTrade(worker, costura, "20000", false)));

        WorkerModel result = workerService.findById(WORKER_ID);

        assertEquals("Plomería", result.getPrimaryTrade().getTrade().getName());
        assertEquals(1, result.getSecondaryTrades().size());
        assertEquals("Costura", result.getSecondaryTrades().get(0).getTrade().getName());
    }

    @Test
    @DisplayName("findById lanza ResourceNotFoundException si el trabajador no existe")
    void shouldFailWhenWorkerNotFound() {
        when(workerRepository.findById(UNKNOWN_WORKER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerService.findById(UNKNOWN_WORKER_ID));
    }

    @Test
    @DisplayName("La búsqueda filtra por zona, disponibilidad, oficio y tarifa máxima")
    void shouldSearchWithFilters() {
        Worker barato = worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE);
        barato.setAvailableNow(true);
        barato.getZoneIds().add(5L);
        Worker caro = worker(OTHER_WORKER_ID, OTHER_USER_ID, WorkerStatus.ACTIVE);
        caro.setAvailableNow(true);
        caro.getZoneIds().add(5L);
        Worker otraZona = worker(UUID.randomUUID(), UNKNOWN_USER_ID, WorkerStatus.ACTIVE);
        otraZona.setAvailableNow(true);
        otraZona.getZoneIds().add(8L);
        when(workerRepository.findByStatus(WorkerStatus.ACTIVE)).thenReturn(List.of(barato, caro, otraZona));
        when(workerTradeRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(workerTrade(barato, plomeria, "40000", true)));
        when(workerTradeRepository.findByWorkerId(OTHER_WORKER_ID)).thenReturn(List.of(workerTrade(caro, plomeria, "90000", true)));

        List<WorkerModel> result = workerService.search(TRADE_ID, 5L, new BigDecimal("50000"), true);

        assertEquals(1, result.size());
        assertEquals(WORKER_ID, result.get(0).getId());
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
        Worker worker = worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE);
        worker.setAvailableNow(true);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        WorkerModel changes = new WorkerModel();
        changes.setStatus(WorkerStatus.PAUSED);
        changes.setAvailableNow(true);
        WorkerModel result = workerService.update(WORKER_ID, changes);

        assertEquals(WorkerStatus.PAUSED, result.getStatus());
        assertFalse(result.isAvailableNow());
    }

    @Test
    @DisplayName("Marca al trabajador como disponible ahora conservando su estado")
    void shouldUpdateAvailableNow() {
        Worker worker = worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        WorkerModel changes = new WorkerModel();
        changes.setAvailableNow(true);
        WorkerModel result = workerService.update(WORKER_ID, changes);

        assertEquals(WorkerStatus.ACTIVE, result.getStatus());
        assertTrue(result.isAvailableNow());
    }

    @Test
    @DisplayName("Un perfil pausado solo se puede reactivar")
    void shouldOnlyReactivatePausedWorker() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker(WORKER_ID, USER_ID, WorkerStatus.PAUSED)));

        WorkerModel changes = new WorkerModel();
        changes.setAvailableNow(true);

        assertThrows(BusinessRuleException.class, () -> workerService.update(WORKER_ID, changes));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactiva un perfil pausado")
    void shouldReactivatePausedWorker() {
        Worker worker = worker(WORKER_ID, USER_ID, WorkerStatus.PAUSED);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        WorkerModel changes = new WorkerModel();
        changes.setStatus(WorkerStatus.ACTIVE);
        assertEquals(WorkerStatus.ACTIVE, workerService.update(WORKER_ID, changes).getStatus());
    }

    // ---- zonas ----

    @Test
    @DisplayName("Agrega una zona de cobertura")
    void shouldAddZone() {
        Worker worker = worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));

        workerService.addZone(WORKER_ID, 5L);

        assertTrue(worker.getZoneIds().contains(5L));
        verify(workerRepository).save(worker);
    }

    @Test
    @DisplayName("Un trabajador pausado no puede cambiar sus zonas")
    void shouldNotAddZoneToPausedWorker() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker(WORKER_ID, USER_ID, WorkerStatus.PAUSED)));

        assertThrows(BusinessRuleException.class, () -> workerService.addZone(WORKER_ID, 5L));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quitar una zona que el trabajador no cubre lanza ResourceNotFoundException")
    void shouldFailRemovingUncoveredZone() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE)));

        assertThrows(ResourceNotFoundException.class, () -> workerService.removeZone(WORKER_ID, 5L));
        verify(workerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quita una zona cubierta y lista las restantes")
    void shouldRemoveZone() {
        Worker worker = worker(WORKER_ID, USER_ID, WorkerStatus.ACTIVE);
        worker.getZoneIds().addAll(Set.of(5L, 6L));
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));

        workerService.removeZone(WORKER_ID, 5L);

        assertEquals(Set.of(6L), workerService.findZones(WORKER_ID));
    }

    @Test
    @DisplayName("Devuelve el perfil del trabajador por su userId")
    void shouldFindByUserId() {
        UUID userId = UUID.randomUUID();
        when(workerRepository.findByUserId(userId))
                .thenReturn(Optional.of(worker(UUID.randomUUID(), userId, WorkerStatus.ACTIVE)));

        assertEquals(userId, workerService.findByUserId(userId).getUserId());
    }

    @Test
    @DisplayName("Lanza 404 si el usuario aún no tiene perfil")
    void shouldFailWhenUserHasNoProfile() {
        UUID userId = UUID.randomUUID();
        when(workerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerService.findByUserId(userId));
    }
}
