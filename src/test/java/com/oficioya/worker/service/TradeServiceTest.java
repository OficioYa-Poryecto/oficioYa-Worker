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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

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
        hogar.setId(1L);
        hogar.setName("Hogar");
    }

    private Trade trade(Long id, String name, boolean active) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setName(name);
        trade.setActive(active);
        trade.setCategory(hogar);
        return trade;
    }

    private TradeModel model(String name, boolean active, Long categoryId) {
        return new TradeModel(null, name, "desc", active, new TradeCategoryModel(categoryId, null, null));
    }

    private void saveReturnsArgument() {
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> {
            Trade saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("Con active=true lista solo los oficios activos")
    void shouldListOnlyActiveTrades() {
        when(tradeRepository.findByActiveTrue()).thenReturn(List.of(trade(1L, "Plomería", true)));

        List<TradeModel> result = tradeService.findAll(true);

        assertEquals(1, result.size());
        verify(tradeRepository, never()).findAll();
    }

    @Test
    @DisplayName("Sin filtro lista todos los oficios")
    void shouldListAllTrades() {
        when(tradeRepository.findAll()).thenReturn(List.of(trade(1L, "Plomería", true), trade(2L, "Costura", false)));

        assertEquals(2, tradeService.findAll(null).size());
        verify(tradeRepository, never()).findByActiveTrue();
    }

    @Test
    @DisplayName("Busca un oficio por id con su categoría")
    void shouldFindTradeById() {
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(trade(1L, "Plomería", true)));

        TradeModel result = tradeService.findById(1L);

        assertEquals("Plomería", result.getName());
        assertEquals("Hogar", result.getCategory().getName());
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el oficio no existe")
    void shouldFailWhenTradeNotFound() {
        when(tradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.findById(99L));
    }

    @Test
    @DisplayName("Crea el oficio siempre activo y con la categoría cargada del repositorio")
    void shouldCreateTradeActive() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(hogar));
        saveReturnsArgument();

        TradeModel result = tradeService.create(model("Electricidad", false, 1L));

        assertTrue(result.isActive(), "los oficios nuevos nacen activos");
        assertEquals(10L, result.getId());
        assertEquals("Hogar", result.getCategory().getName());
    }

    @Test
    @DisplayName("No crea un oficio con una categoría inexistente")
    void shouldNotCreateTradeWithUnknownCategory() {
        when(categoryRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.create(model("Electricidad", true, 7L)));
        verify(tradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualiza nombre, descripción, estado y categoría")
    void shouldUpdateTrade() {
        TradeCategory educacion = new TradeCategory();
        educacion.setId(2L);
        educacion.setName("Educación");
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(trade(1L, "Plomería", true)));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(educacion));
        saveReturnsArgument();

        TradeModel result = tradeService.update(1L, model("Clases de matemáticas", false, 2L));

        assertEquals("Clases de matemáticas", result.getName());
        assertFalse(result.isActive());
        assertEquals("Educación", result.getCategory().getName());
    }

    @Test
    @DisplayName("No actualiza un oficio inexistente")
    void shouldNotUpdateUnknownTrade() {
        when(tradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradeService.update(99L, model("X", true, 1L)));
        verify(tradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar marca el oficio como inactivo sin borrarlo")
    void shouldDeactivateTrade() {
        Trade plomeria = trade(1L, "Plomería", true);
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(plomeria));

        tradeService.deactivate(1L);

        assertFalse(plomeria.isActive());
        verify(tradeRepository).save(plomeria);
        verify(tradeRepository, never()).delete(any());
    }
}
