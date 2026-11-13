package com.oficioya.worker.repository;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TradeRepositoryTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TradeCategoryRepository categoryRepository;

    private TradeCategory saveCategory() {
        TradeCategory category = new TradeCategory();
        category.setName("General");
        category.setDetail("Oficios generales");
        return categoryRepository.save(category);
    }

    private Trade newTrade(String name, boolean active, TradeCategory category) {
        Trade trade = new Trade();
        trade.setName(name);
        trade.setDescription("Descripcion de " + name);
        trade.setActive(active);
        trade.setCategory(category);
        return trade;
    }

    @Test
    @DisplayName("Debe guardar un oficio con su categoria")
    void shouldSaveTradeWithCategory() {
        TradeCategory category = saveCategory();

        Trade saved = tradeRepository.save(newTrade("Plomero", true, category));

        assertNotNull(saved.getId());
        assertEquals(category.getId(), saved.getCategory().getId());
    }

    @Test
    @DisplayName("Debe buscar oficios por nombre ignorando mayusculas")
    void shouldFindByNameContainingIgnoreCase() {
        TradeCategory category = saveCategory();
        tradeRepository.save(newTrade("Plomero", true, category));
        tradeRepository.save(newTrade("Electricista", true, category));

        List<Trade> result = tradeRepository.findByNameContainingIgnoreCase("plom");

        assertEquals(1, result.size());
        assertEquals("Plomero", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe listar solo los oficios activos")
    void shouldFindActiveTrades() {
        TradeCategory category = saveCategory();
        tradeRepository.save(newTrade("Plomero", true, category));
        tradeRepository.save(newTrade("Pintor", false, category));

        assertEquals(1, tradeRepository.findByActiveTrue().size());
    }
}