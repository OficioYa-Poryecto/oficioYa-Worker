package com.oficioya.worker.mapper;

import com.oficioya.worker.dto.WorkPhotoRequest;
import com.oficioya.worker.dto.WorkPhotoResponse;
import com.oficioya.worker.model.WorkPhotoModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkPhotoDtoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)   // lo asigna el service
    WorkPhotoModel toModel(WorkPhotoRequest request);

    WorkPhotoResponse toResponse(WorkPhotoModel model);

    List<WorkPhotoResponse> toResponseList(List<WorkPhotoModel> models);
}
