package com.oficioya.worker.mapper;

import com.oficioya.worker.entity.WorkerTrade;
import com.oficioya.worker.model.WorkerTradeModel;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = TradeMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WorkerTradeMapper {
    WorkerTradeModel toModel(WorkerTrade entity);

    @Mapping(target = "worker", ignore = true)   // lo asigna el service
    @Mapping(target = "primary", ignore = true)  // lo asigna el service
    WorkerTrade toEntity(WorkerTradeModel model);

    List<WorkerTradeModel> toModelList(List<WorkerTrade> entities);
}