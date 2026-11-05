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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerTradeServiceTest {

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
        worker.setId(1L);
        worker.setUserId(10L);
        worker.setStatus(WorkerStatus.ACTIVE);

        TradeCategory hogar = new TradeCategory();
        hogar.setId(1L);
        hogar.setName("Hogar");
        plomeria = trade(3L, "Plomería", hogar);
        costura = trade(4L, "Costura", hogar);
    }

    private Trade trade(Long id, String name, TradeCategory category) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setName(name);
        trade.setActive(true);
        trade.setCategory(category);
        return trade;
    }

    private WorkerTrade workerTrade(Long id, Worker owner, Trade trade, boolean primary) {
        WorkerTrade workerTrade = new WorkerTrade();
        workerTrade.setId(id);
        workerTrade.setWorker(owner);
        workerTrade.setTrade(trade);
        workerTrade.setPrimary(primary);
        workerTrade.setApproxRate(new BigDecimal("50000"));
        workerTrade.setRateUnit(RateUnit.PER_JOB);
        return workerTrade;
    }

    private WorkerTradeModel request(Long tradeId, String rate) {
        TradeModel trade = new TradeModel();
        trade.setId(tradeId);
        return new WorkerTradeModel(null, new BigDecimal(rate), RateUnit.PER_HOUR, trade);
    }

    // ---- addSecondary ----

    @Test
    @DisplayName("Agrega un oficio secundario con su tarifa")
    void shouldAddSecondaryTrade() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(4L)).thenReturn(Optional.of(costura));
        when(workerTradeRepository.findByWorkerId(1L)).thenReturn(List.of(workerTrade(11L, worker, plomeria, true)));
        when(workerTradeRepository.save(any(WorkerTrade.class))).thenAnswer(inv -> {
            WorkerTrade saved = inv.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        WorkerTradeModel result = workerTradeService.addSecondary(1L, request(4L, "20000"));

        ArgumentCaptor<WorkerTrade> captor = ArgumentCaptor.forClass(WorkerTrade.class);
        verify(workerTradeRepository).save(captor.capture());
        assertFalse(captor.getValue().isPrimary());
        assertSame(worker, captor.getValue().getWorker());
        assertEquals(12L, result.getId());
        assertEquals("Costura", result.getTrade().getName());
        assertEquals(new BigDecimal("20000"), result.getApproxRate());
    }

    @Test
    @DisplayName("No agrega un oficio que el trabajador ya ofrece")
    void shouldRejectDuplicatedTrade() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(3L)).thenReturn(Optional.of(plomeria));
        when(workerTradeRepository.findByWorkerId(1L)).thenReturn(List.of(workerTrade(11L, worker, plomeria, true)));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.addSecondary(1L, request(3L, "1000")));
        verify(workerTradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("No agrega un oficio inactivo del catálogo")
    void shouldRejectInactiveTrade() {
        costura.setActive(false);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(4L)).thenReturn(Optional.of(costura));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.addSecondary(1L, request(4L, "1000")));
        verify(workerTradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("No agrega un oficio que no existe en el catálogo")
    void shouldRejectUnknownTrade() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(tradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerTradeService.addSecondary(1L, request(99L, "1000")));
    }

    @Test
    @DisplayName("Un trabajador pausado no puede agregar oficios")
    void shouldRejectPausedWorker() {
        worker.setStatus(WorkerStatus.PAUSED);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.addSecondary(1L, request(4L, "1000")));
        verifyNoInteractions(tradeRepository, workerTradeRepository);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el trabajador no existe")
    void shouldRejectUnknownWorker() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workerTradeService.addSecondary(99L, request(4L, "1000")));
    }

    // ---- update (tarifa) ----

    @Test
    @DisplayName("Actualiza la tarifa y la unidad sin cambiar el oficio")
    void shouldUpdateRate() {
        WorkerTrade existing = workerTrade(11L, worker, plomeria, true);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(workerTradeRepository.save(existing)).thenReturn(existing);

        WorkerTradeModel result = workerTradeService.update(1L, 11L, request(null, "65000"));

        assertEquals(new BigDecimal("65000"), result.getApproxRate());
        assertEquals(RateUnit.PER_HOUR, result.getRateUnit());
        assertEquals("Plomería", result.getTrade().getName());
    }

    @Test
    @DisplayName("No permite modificar un oficio que pertenece a otro trabajador")
    void shouldNotUpdateTradeOfAnotherWorker() {
        Worker otro = new Worker();
        otro.setId(2L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(21L)).thenReturn(Optional.of(workerTrade(21L, otro, plomeria, true)));

        assertThrows(ResourceNotFoundException.class, () -> workerTradeService.update(1L, 21L, request(null, "1")));
        verify(workerTradeRepository, never()).save(any());
    }

    // ---- changePrimary ----

    @Test
    @DisplayName("Cambia el oficio principal y el anterior pasa a secundario")
    void shouldChangePrimaryTrade() {
        WorkerTrade actual = workerTrade(11L, worker, plomeria, true);
        WorkerTrade nuevo = workerTrade(12L, worker, costura, false);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(12L)).thenReturn(Optional.of(nuevo));
        when(workerTradeRepository.findByWorkerIdAndPrimaryTrue(1L)).thenReturn(Optional.of(actual));

        WorkerTradeModel result = workerTradeService.changePrimary(1L, 12L);

        assertFalse(actual.isPrimary());
        assertTrue(nuevo.isPrimary());
        assertEquals(12L, result.getId());
        verify(workerTradeRepository).save(actual);
        verify(workerTradeRepository).save(nuevo);
    }

    @Test
    @DisplayName("Si el oficio ya es el principal no hace cambios")
    void shouldKeepPrimaryWhenAlreadyPrimary() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(11L)).thenReturn(Optional.of(workerTrade(11L, worker, plomeria, true)));

        workerTradeService.changePrimary(1L, 11L);

        verify(workerTradeRepository, never()).save(any());
    }

    // ---- deleteSecondary ----

    @Test
    @DisplayName("Elimina un oficio secundario")
    void shouldDeleteSecondaryTrade() {
        WorkerTrade secundario = workerTrade(12L, worker, costura, false);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(12L)).thenReturn(Optional.of(secundario));

        workerTradeService.deleteSecondary(1L, 12L);

        verify(workerTradeRepository).delete(secundario);
    }

    @Test
    @DisplayName("No permite eliminar el oficio principal")
    void shouldNotDeletePrimaryTrade() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerTradeRepository.findById(11L)).thenReturn(Optional.of(workerTrade(11L, worker, plomeria, true)));

        assertThrows(BusinessRuleException.class, () -> workerTradeService.deleteSecondary(1L, 11L));
        verify(workerTradeRepository, never()).delete(any());
    }
}
