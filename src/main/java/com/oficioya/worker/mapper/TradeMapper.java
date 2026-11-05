package com.oficioya.worker.mapper;

import com.oficioya.worker.entity.Trade;
import com.oficioya.worker.model.TradeModel;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = TradeCategoryMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TradeMapper {
    TradeModel toModel(Trade entity);
    Trade toEntity(TradeModel model);
    List<TradeModel> toModelList(List<Trade> entities);
}