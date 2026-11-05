package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.WorkerCreateRequest;
import com.oficioya.worker.dto.WorkerResponse;
import com.oficioya.worker.dto.WorkerUpdateRequest;
import com.oficioya.worker.model.WorkerModel;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {WorkerTradeDtoMapper.class, AvailabilitySlotDtoMapper.class, WorkPhotoDtoMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WorkerDtoMapper {

    // Estado, oficios secundarios, franjas y fotos se gestionan con sus propios endpoints
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "secondaryTrades", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "photos", ignore = true)
    WorkerModel toModel(WorkerCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "zoneIds", ignore = true)
    @Mapping(target = "primaryTrade", ignore = true)
    @Mapping(target = "secondaryTrades", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "photos", ignore = true)
    WorkerModel toModel(WorkerUpdateRequest request);

    WorkerResponse toResponse(WorkerModel model);

    List<WorkerResponse> toResponseList(List<WorkerModel> models);
}
