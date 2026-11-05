package com.oficioya.worker.service;

import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.exception.BusinessRuleException;
import com.oficioya.worker.mapper.TradeCategoryMapperImpl;
import com.oficioya.worker.model.TradeCategoryModel;
import com.oficioya.worker.repository.TradeCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeCategoryServiceTest {

    @Mock
    private TradeCategoryRepository categoryRepository;

    private TradeCategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new TradeCategoryService(categoryRepository, new TradeCategoryMapperImpl());
    }

    private TradeCategory category(Long id, String name) {
        TradeCategory category = new TradeCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    @Test
    @DisplayName("Lista todas las categorías")
    void shouldListCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Hogar"), category(2L, "Educación")));

        List<TradeCategoryModel> result = categoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("Hogar", result.get(0).getName());
    }

    @Test
    @DisplayName("Crea una categoría nueva ignorando el id que venga en el modelo")
    void shouldCreateCategory() {
        when(categoryRepository.findByNameIgnoreCase("Hogar")).thenReturn(Optional.empty());
        List<Long> idsAlGuardar = new ArrayList<>();
        when(categoryRepository.save(any(TradeCategory.class))).thenAnswer(inv -> {
            TradeCategory saved = inv.getArgument(0);
            idsAlGuardar.add(saved.getId());   // se registra antes de que la "BD" asigne el id
            saved.setId(5L);
            return saved;
        });

        TradeCategoryModel result = categoryService.create(new TradeCategoryModel(99L, "Hogar", "Arreglos"));

        assertEquals(1, idsAlGuardar.size());
        assertNull(idsAlGuardar.get(0), "no se debe poder fijar el id desde afuera");
        assertEquals(5L, result.getId());
        assertEquals("Arreglos", result.getDetail());
    }

    @Test
    @DisplayName("No crea una categoría con un nombre que ya existe (sin importar mayúsculas)")
    void shouldRejectDuplicatedCategory() {
        when(categoryRepository.findByNameIgnoreCase("hogar")).thenReturn(Optional.of(category(1L, "Hogar")));

        assertThrows(BusinessRuleException.class,
                () -> categoryService.create(new TradeCategoryModel(null, "hogar", null)));
        verify(categoryRepository, never()).save(any());
    }
}
