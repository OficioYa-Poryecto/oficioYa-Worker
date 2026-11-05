package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.TradeCategoryRequest;
import com.oficioya.worker.dto.TradeCategoryResponse;
import com.oficioya.worker.model.TradeCategoryModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TradeCategoryDtoMapper {
    @Mapping(target = "id", ignore = true)
    TradeCategoryModel toModel(TradeCategoryRequest request);

    TradeCategoryResponse toResponse(TradeCategoryModel model);

    List<TradeCategoryResponse> toResponseList(List<TradeCategoryModel> models);
}
