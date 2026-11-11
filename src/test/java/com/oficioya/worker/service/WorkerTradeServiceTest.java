package com.oficioya.worker.service;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.entity.WorkerTrade;
import com.oficioya.worker.exception.BusinessRuleException;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.TradeCategoryMapperImpl;
import com.oficioya.worker.mapper.TradeMapperImpl;
import com.oficioya.worker.mapper.WorkerTradeMapperImpl;
import com.oficioya.worker.model.RateUnit;
import com.oficioya.worker.model.TradeModel;
import com.oficioya.worker.model.WorkerStatus;
import com.oficioya.worker.model.WorkerTradeModel;
import com.oficioya.worker.repository.TradeRepository;
import com.oficioya.worker.repository.WorkerRepository;
import com.oficioya.worker.repository.WorkerTradeRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerTradeServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID OTHER_WORKER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_WORKER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PLOMERIA_ID = UUID.randomUUID();
    private static final UUID COSTURA_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_TRADE_ID = UUID.randomUUID();
    private static final UUID PRIMARY_WORKER_TRADE_ID = UUID.randomUUID();
    private static final UUID SECONDARY_WORKER_TRADE_ID = UUID.randomUUID();
    private static final UUID UPDATED_WORKER_TRADE_ID = UUID.randomUUID();

    @Mock private WorkerRepository workerRepository;
    @Mock private WorkerTradeRepository workerTradeRepository;
    @Mock private TradeRepository tradeRepository;

    private WorkerTradeService workerTradeService;
    private Worker worker;
    private Trade plomeria;
    private Trade costura;

    @BeforeEach
    void setUp() {
        workerTradeService = new WorkerTradeService(workerRepository, workerTradeRepository, tradeRepository,
                new WorkerTradeMapperImpl(new TradeMapperImpl(new TradeCategoryMapperImpl())));

        worker = new Worker();
        worker.setId(WORKER_ID);
        worker.setUserId(USER_ID);
        worker.setStatus(WorkerStatus.ACTIVE);

        TradeCategory hogar = new TradeCategory();
        hogar.setId(CATEGORY_ID);
        hogar.setName("Hogar");
        plomeria = trade(PLOMERIA_ID, "Plomería", hogar);
        costura = trade(COSTURA_ID, "Costura", hogar);
    }

    private Trade trade(UUID id, String name, TradeCategory category) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setName(name);
        trade.setActive(true);
        trade.setCategory(category);
        return trade;
    }

    private WorkerTrade workerTrade(UUID id, Worker owner, Trade trade, boolean primary) {
        WorkerTrade workerTrade = new WorkerTrade();
        workerTrade.setId(id);
        workerTrade.setWorker(owner);
        workerTrade.setTrade(trade);
        workerTrade.setPrimary(primary);
        workerTrade.setApproxRate(new BigDecimal("50000"));
        workerTrade.setRateUnit(RateUnit.PER_JOB);
        return workerTrade;
    }

    private WorkerTradeModel request(UUID tradeId, String rate) {
        TradeModel trade = new TradeModel();
        trade.setId(tradeId);
        return new WorkerTradeModel(null, new BigDecimal(rate), RateUnit.PER_HOUR, trade);
    }

    // ---- addSecondary ----

    @Test
    @DisplayName("Agrega un oficio secundario con su tarifa")
    void shouldAddSecondaryTrade() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(COSTURA_ID)).thenReturn(Optional.of(costura));
        when(workerTradeRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(workerTrade(PRIMARY_WORKER_TRADE_ID, worker, plomeria, true)));
        when(workerTradeRepository.save(any(WorkerTrade.class))).thenAnswer(inv -> {
            WorkerTrade saved = inv.getArgument(0);
            saved.setId(SECONDARY_WORKER_TRADE_ID);
            return saved;
        });

        WorkerTradeModel result = workerTradeService.addSecondary(WORKER_ID, request(COSTURA_ID, "20000"));

        ArgumentCaptor<WorkerTrade> captor = ArgumentCaptor.forClass(WorkerTrade.class);
        verify(workerTradeRepository).save(captor.capture());
        assertFalse(captor.getValue().isPrimary());
        assertSame(worker, captor.getValue().getWorker());
        assertEquals(SECONDARY_WORKER_TRADE_ID, result.getId());
        assertEquals("Costura", result.getTrade().getName());
        assertEquals(new BigDecimal("20000"), result.getApproxRate());
    }

    @Test
    @DisplayName("No agrega un oficio que el trabajador ya ofrece")
    void shouldRejectDuplicatedTrade() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(PLOMERIA_ID)).thenReturn(Optional.of(plomeria));
        when(workerTradeRepository.findByWorkerId(WORKER_ID)).thenReturn(List.of(workerTrade(PRIMARY_WORKER_TRADE_ID, worker, plomeria, true)));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.addSecondary(WORKER_ID, request(PLOMERIA_ID, "1000")));
        verify(workerTradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("No agrega un oficio inactivo del catálogo")
    void shouldRejectInactiveTrade() {
        costura.setActive(false);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(COSTURA_ID)).thenReturn(Optional.of(costura));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.addSecondary(WORKER_ID, request(COSTURA_ID, "1000")));
        verify(workerTradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("No agrega un oficio que no existe en el catálogo")
    void shouldRejectUnknownTrade() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(UNKNOWN_TRADE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerTradeService.addSecondary(WORKER_ID, request(UNKNOWN_TRADE_ID, "1000")));
    }

    @Test
    @DisplayName("Un trabajador pausado no puede agregar oficios")
    void shouldRejectPausedWorker() {
        worker.setStatus(WorkerStatus.PAUSED);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.addSecondary(WORKER_ID, request(COSTURA_ID, "1000")));
        verifyNoInteractions(tradeRepository, workerTradeRepository);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el trabajador no existe")
    void shouldRejectUnknownWorker() {
        when(workerRepository.findById(UNKNOWN_WORKER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerTradeService.addSecondary(UNKNOWN_WORKER_ID, request(COSTURA_ID, "1000")));
    }

    // ---- update (tarifa) ----

    @Test
    @DisplayName("Actualiza la tarifa y la unidad sin cambiar el oficio")
    void shouldUpdateRate() {
        WorkerTrade existing = workerTrade(PRIMARY_WORKER_TRADE_ID, worker, plomeria, true);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(PRIMARY_WORKER_TRADE_ID)).thenReturn(Optional.of(existing));
        when(workerTradeRepository.save(existing)).thenReturn(existing);

        WorkerTradeModel result = workerTradeService.update(WORKER_ID, PRIMARY_WORKER_TRADE_ID, request(null, "65000"));

        assertEquals(new BigDecimal("65000"), result.getApproxRate());
        assertEquals(RateUnit.PER_HOUR, result.getRateUnit());
        assertEquals("Plomería", result.getTrade().getName());
    }

    @Test
    @DisplayName("No permite modificar un oficio que pertenece a otro trabajador")
    void shouldNotUpdateTradeOfAnotherWorker() {
        Worker otro = new Worker();
        otro.setId(OTHER_WORKER_ID);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(UPDATED_WORKER_TRADE_ID)).thenReturn(Optional.of(workerTrade(UPDATED_WORKER_TRADE_ID, otro, plomeria, true)));

        assertThrows(ResourceNotFoundException.class, () -> workerTradeService.update(WORKER_ID, UPDATED_WORKER_TRADE_ID, request(null, "1")));
        verify(workerTradeRepository, never()).save(any());
    }

    // ---- changePrimary ----

    @Test
    @DisplayName("Cambia el oficio principal y el anterior pasa a secundario")
    void shouldChangePrimaryTrade() {
        WorkerTrade actual = workerTrade(PRIMARY_WORKER_TRADE_ID, worker, plomeria, true);
        WorkerTrade nuevo = workerTrade(SECONDARY_WORKER_TRADE_ID, worker, costura, false);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(SECONDARY_WORKER_TRADE_ID)).thenReturn(Optional.of(nuevo));
        when(workerTradeRepository.findByWorkerIdAndPrimaryTrue(WORKER_ID)).thenReturn(Optional.of(actual));

        WorkerTradeModel result = workerTradeService.changePrimary(WORKER_ID, SECONDARY_WORKER_TRADE_ID);

        assertFalse(actual.isPrimary());
        assertTrue(nuevo.isPrimary());
        assertEquals(SECONDARY_WORKER_TRADE_ID, result.getId());
        verify(workerTradeRepository).save(actual);
        verify(workerTradeRepository).save(nuevo);
    }

    @Test
    @DisplayName("Si el oficio ya es el principal no hace cambios")
    void shouldKeepPrimaryWhenAlreadyPrimary() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(PRIMARY_WORKER_TRADE_ID)).thenReturn(Optional.of(workerTrade(PRIMARY_WORKER_TRADE_ID, worker, plomeria, true)));

        workerTradeService.changePrimary(WORKER_ID, PRIMARY_WORKER_TRADE_ID);

        verify(workerTradeRepository, never()).save(any());
    }

    // ---- deleteSecondary ----

    @Test
    @DisplayName("Elimina un oficio secundario")
    void shouldDeleteSecondaryTrade() {
        WorkerTrade secundario = workerTrade(SECONDARY_WORKER_TRADE_ID, worker, costura, false);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(SECONDARY_WORKER_TRADE_ID)).thenReturn(Optional.of(secundario));

        workerTradeService.deleteSecondary(WORKER_ID, SECONDARY_WORKER_TRADE_ID);

        verify(workerTradeRepository).delete(secundario);
    }

    @Test
    @DisplayName("No permite eliminar el oficio principal")
    void shouldNotDeletePrimaryTrade() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(PRIMARY_WORKER_TRADE_ID)).thenReturn(Optional.of(workerTrade(PRIMARY_WORKER_TRADE_ID, worker, plomeria, true)));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.deleteSecondary(WORKER_ID, PRIMARY_WORKER_TRADE_ID));
        verify(workerTradeRepository, never()).delete(any());
    }
}
