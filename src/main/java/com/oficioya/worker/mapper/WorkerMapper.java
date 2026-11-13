package com.oficioya.worker.mapper;

import com.oficioya.worker.entity.Worker;
import com.oficioya.worker.model.WorkerModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkerMapper {

    // Trades, slots y fotos los carga el service desde sus repositorios
    @Mapping(target = "primaryTrade", ignore = true)
    @Mapping(target = "secondaryTrades", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "photos", ignore = true)
    WorkerModel toModel(Worker entity);

    Worker toEntity(WorkerModel model);

    List<WorkerModel> toModelList(List<Worker> entities);
}