package com.fivevision.api.media.internal.repository;

import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository
        extends JpaRepository<MediaAsset, UUID>, JpaSpecificationExecutor<MediaAsset> {
    List<MediaAsset> findByStatusAndCreatedAtBefore(MediaStatus status, OffsetDateTime cutoff);
    List<MediaAsset> findAllByUploaderId(UUID uploaderId);
}