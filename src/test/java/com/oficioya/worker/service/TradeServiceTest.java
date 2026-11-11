package com.oficioya.worker.service;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.exception.ResourceNotFoundException;
import com.oficioya.worker.mapper.TradeCategoryMapperImpl;
import com.oficioya.worker.mapper.TradeMapperImpl;
import com.oficioya.worker.model.TradeCategoryModel;
import com.oficioya.worker.model.TradeModel;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OTHER_CATEGORY_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_CATEGORY_ID = UUID.randomUUID();
    private static final UUID TRADE_ID = UUID.randomUUID();
    private static final UUID OTHER_TRADE_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_TRADE_ID = UUID.randomUUID();
    private static final UUID SAVED_TRADE_ID = UUID.randomUUID();

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeCategoryRepository categoryRepository;

    private TradeService tradeService;
    private TradeCategory hogar;

    @BeforeEach
    void setUp() {
        tradeService = new TradeService(tradeRepository, categoryRepository,
                new TradeMapperImpl(new TradeCategoryMapperImpl()));
        hogar = new TradeCategory();
        hogar.setId(CATEGORY_ID);
        hogar.setName("Hogar");
    }

    private Trade trade(UUID id, String name, boolean active) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setName(name);
        trade.setActive(active);
        trade.setCategory(hogar);
        return trade;
    }

    private TradeModel model(String name, boolean active, UUID categoryId) {
        return new TradeModel(null, name, "desc", active, new TradeCategoryModel(categoryId, null, null));
    }

    private void saveReturnsArgument() {
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> {
            Trade saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(SAVED_TRADE_ID);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("Con active=true lista solo los oficios activos")
    void shouldListOnlyActiveTrades() {
        when(tradeRepository.findByActiveTrue()).thenReturn(List.of(trade(TRADE_ID, "Plomería", true)));

        List<TradeModel> result = tradeService.findAll(true);

        assertEquals(1, result.size());
        verify(tradeRepository, never()).findAll();
    }

    @Test
    @DisplayName("Sin filtro lista todos los oficios")
    void shouldListAllTrades() {
        when(tradeRepository.findAll()).thenReturn(List.of(trade(TRADE_ID, "Plomería", true), trade(OTHER_TRADE_ID, "Costura", false)));

        assertEquals(2, tradeService.findAll(null).size());
        verify(tradeRepository, never()).findByActiveTrue();
    }

    @Test
    @DisplayName("Busca un oficio por id con su categoría")
    void shouldFindTradeById() {
        when(tradeRepository.findById(TRADE_ID)).thenReturn(Optional.of(trade(TRADE_ID, "Plomería", true)));

        TradeModel result = tradeService.findById(TRADE_ID);

        assertEquals("Plomería", result.getName());
        assertEquals("Hogar", result.getCategory().getName());
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el oficio no existe")
    void shouldFailWhenTradeNotFound() {
        when(tradeRepository.findById(UNKNOWN_TRADE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.findById(UNKNOWN_TRADE_ID));
    }

    @Test
    @DisplayName("Crea el oficio siempre activo y con la categoría cargada del repositorio")
    void shouldCreateTradeActive() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(hogar));
        saveReturnsArgument();

        TradeModel result = tradeService.create(model("Electricidad", false, CATEGORY_ID));

        assertTrue(result.isActive(), "los oficios nuevos nacen activos");
        assertEquals(SAVED_TRADE_ID, result.getId());
        assertEquals("Hogar", result.getCategory().getName());
    }

    @Test
    @DisplayName("No crea un oficio con una categoría inexistente")
    void shouldNotCreateTradeWithUnknownCategory() {
        when(categoryRepository.findById(UNKNOWN_CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.create(model("Electricidad", true, UNKNOWN_CATEGORY_ID)));
        verify(tradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualiza nombre, descripción, estado y categoría")
    void shouldUpdateTrade() {
        TradeCategory educacion = new TradeCategory();
        educacion.setId(OTHER_CATEGORY_ID);
        educacion.setName("Educación");
        when(tradeRepository.findById(TRADE_ID)).thenReturn(Optional.of(trade(TRADE_ID, "Plomería", true)));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(educacion));
        saveReturnsArgument();

        TradeModel result = tradeService.update(TRADE_ID, model("Clases de matemáticas", false, OTHER_CATEGORY_ID));

        assertEquals("Clases de matemáticas", result.getName());
        assertFalse(result.isActive());
        assertEquals("Educación", result.getCategory().getName());
    }

    @Test
    @DisplayName("No actualiza un oficio inexistente")
    void shouldNotUpdateUnknownTrade() {
        when(tradeRepository.findById(UNKNOWN_TRADE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.update(UNKNOWN_TRADE_ID, model("X", true, CATEGORY_ID)));
        verify(tradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactiva un oficio sin eliminarlo")
    void shouldDeactivateTrade() {
        UUID id = UUID.randomUUID();
        when(tradeRepository.findById(id)).thenReturn(Optional.of(trade(id, "Plomería", true)));
        saveReturnsArgument();

        TradeModel result = tradeService.setActive(id, false);

        assertFalse(result.isActive());
        verify(tradeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Reactiva un oficio inactivo")
    void shouldReactivateTrade() {
        UUID id = UUID.randomUUID();
        when(tradeRepository.findById(id)).thenReturn(Optional.of(trade(id, "Plomería", false)));
        saveReturnsArgument();

        assertTrue(tradeService.setActive(id, true).isActive());
    }

    @Test
    @DisplayName("Lanza 404 al cambiar el estado de un oficio que no existe")
    void shouldFailSetActiveWhenTradeNotFound() {
        UUID id = UUID.randomUUID();
        when(tradeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.setActive(id, false));
    }
}
