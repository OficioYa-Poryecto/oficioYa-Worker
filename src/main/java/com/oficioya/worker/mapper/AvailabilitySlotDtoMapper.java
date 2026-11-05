package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.AvailabilitySlotRequest;
import com.oficioya.worker.dto.AvailabilitySlotResponse;
import com.oficioya.worker.model.AvailabilitySlotModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AvailabilitySlotDtoMapper {
    @Mapping(target = "id", ignore = true)
    AvailabilitySlotModel toModel(AvailabilitySlotRequest request);

    AvailabilitySlotResponse toResponse(AvailabilitySlotModel model);

    List<AvailabilitySlotResponse> toResponseList(List<AvailabilitySlotModel> models);
}
