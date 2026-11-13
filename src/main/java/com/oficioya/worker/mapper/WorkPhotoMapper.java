package com.oficioya.worker.mapper;

import com.oficioya.worker.entity.WorkPhoto;
import com.oficioya.worker.model.WorkPhotoModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkPhotoMapper {
    WorkPhotoModel toModel(WorkPhoto entity);

    @Mapping(target = "worker", ignore = true)
    WorkPhoto toEntity(WorkPhotoModel model);

    List<WorkPhotoModel> toModelList(List<WorkPhoto> entities);
}