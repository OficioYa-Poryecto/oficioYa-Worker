package com.oficioya.worker.mapper;

import com.oficioya.worker.entity.TradeCategory;
import com.oficioya.worker.model.TradeCategoryModel;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TradeCategoryMapper {
    TradeCategoryModel toModel(TradeCategory entity);
    TradeCategory toEntity(TradeCategoryModel model);
    List<TradeCategoryModel> toModelList(List<TradeCategory> entities);
}