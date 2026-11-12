package com.oficioya.worker.repository;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.entity.WorkerTrade;
import com.oficioya.worker.model.RateUnit;
import com.oficioya.worker.model.WorkerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class WorkerTradeRepositoryTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private WorkerTradeRepository workerTradeRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TradeCategoryRepository categoryRepository;

    private Worker saveWorker() {
        Worker worker = new Worker();
        worker.setUserId(USER_ID);
        worker.setStatus(WorkerStatus.ACTIVE);
        worker.setAvailableNow(false);
        return workerRepository.save(worker);
    }

    private Trade saveTrade(String name) {
        TradeCategory category = new TradeCategory();
        category.setName("General " + name);
        category = categoryRepository.save(category);

        Trade trade = new Trade();
        trade.setName(name);
        trade.setActive(true);
        trade.setCategory(category);
        return tradeRepository.save(trade);
    }

    private WorkerTrade newWorkerTrade(Worker worker, Trade trade, boolean primary) {
        WorkerTrade workerTrade = new WorkerTrade();
        workerTrade.setWorker(worker);
        workerTrade.setTrade(trade);
        workerTrade.setPrimary(primary);
        workerTrade.setApproxRate(new BigDecimal("50000.00"));
        workerTrade.setRateUnit(RateUnit.PER_HOUR);
        return workerTrade;
    }

    @Test
    @DisplayName("Debe listar los oficios de un worker (relacion)")
    void shouldFindByWorkerId() {
        Worker worker = saveWorker();
        workerTradeRepository.save(newWorkerTrade(worker, saveTrade("Plomero"), true));
        workerTradeRepository.save(newWorkerTrade(worker, saveTrade("Pintor"), false));

        List<WorkerTrade> result = workerTradeRepository.findByWorkerId(worker.getId());

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Debe encontrar el oficio principal de un worker")
    void shouldFindPrimaryTrade() {
        Worker worker = saveWorker();
        Trade plomero = saveTrade("Plomero");
        workerTradeRepository.save(newWorkerTrade(worker, plomero, true));
        workerTradeRepository.save(newWorkerTrade(worker, saveTrade("Pintor"), false));

        WorkerTrade primary = workerTradeRepository.findByWorkerIdAndPrimaryTrue(worker.getId()).get();

        assertEquals(plomero.getId(), primary.getTrade().getId());
    }
}