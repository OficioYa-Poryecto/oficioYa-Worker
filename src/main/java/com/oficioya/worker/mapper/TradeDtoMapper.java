package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.TradeRequest;
import com.oficioya.worker.dto.TradeResponse;
import com.oficioya.worker.model.TradeCategoryModel;
import com.oficioya.worker.model.TradeModel;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = TradeCategoryDtoMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TradeDtoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", source = "active", defaultValue = "true")
    @Mapping(target = "category", source = "categoryId")
    TradeModel toModel(TradeRequest request);

    TradeResponse toResponse(TradeModel model);

    List<TradeResponse> toResponseList(List<TradeModel> models);

    // El request solo trae el id de la categoría; el service la carga del repositorio
    default TradeCategoryModel categoryFromId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        TradeCategoryModel category = new TradeCategoryModel();
        category.setId(categoryId);
        return category;
    }
}
