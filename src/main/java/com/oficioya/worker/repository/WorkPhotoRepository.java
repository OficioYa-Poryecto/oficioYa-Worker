package com.oficioya.worker.repository;

import com.oficioya.worker.entity.WorkPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkPhotoRepository extends JpaRepository<WorkPhoto, UUID> {

    List<WorkPhoto> findByWorkerId(UUID workerId);
}