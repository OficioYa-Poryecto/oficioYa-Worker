package com.oficioya.worker.config;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.repository.TradeCategoryRepository;
import com.oficioya.worker.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSeederTest {

    @Mock
    private TradeCategoryRepository categoryRepository;
    @Mock
    private TradeRepository tradeRepository;
    @InjectMocks
    private CatalogSeeder seeder;

    @Test
    @DisplayName("Carga 11 categorías y 39 oficios activos cuando el catálogo está vacío")
    void shouldSeedWhenEmpty() {
        when(categoryRepository.count()).thenReturn(0L);
        when(categoryRepository.save(any(TradeCategory.class))).then(returnsFirstArg());

        seeder.run(null);

        ArgumentCaptor<Trade> trades = ArgumentCaptor.forClass(Trade.class);
        verify(categoryRepository, times(11)).save(any(TradeCategory.class));
        verify(tradeRepository, times(39)).save(trades.capture());
        assertTrue(trades.getAllValues().stream()
                .allMatch(t -> t.isActive() && t.getCategory() != null));
    }

    @Test
    @DisplayName("No hace nada si ya hay categorías")
    void shouldSkipWhenCatalogExists() {
        when(categoryRepository.count()).thenReturn(3L);

        seeder.run(null);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(tradeRepository);
    }
}