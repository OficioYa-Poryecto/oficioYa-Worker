package com.oficioya.worker.mapper;

import com.oficioya.worker.entity.AvailabilitySlot;
import com.oficioya.worker.model.AvailabilitySlotModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AvailabilitySlotMapper {
    AvailabilitySlotModel toModel(AvailabilitySlot entity);

    @Mapping(target = "worker", ignore = true)
    AvailabilitySlot toEntity(AvailabilitySlotModel model);

    List<AvailabilitySlotModel> toModelList(List<AvailabilitySlot> entities);
}