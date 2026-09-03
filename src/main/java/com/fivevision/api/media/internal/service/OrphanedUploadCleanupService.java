package com.fivevision.api.media.internal.service;

import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrphanedUploadCleanupService {

    private final MediaAssetRepository repository;
    private final S3Client s3Client;

    @Value("${media.orphan-upload-cleanup.threshold-hours:24}")
    private int thresholdHours;

    @Scheduled(fixedDelayString = "${media.orphan-upload-cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupOrphanedUploads() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(thresholdHours);
        List<MediaAsset> staleAssets = repository.findByStatusAndCreatedAtBefore(
                MediaStatus.PROCESSING, cutoff);

        if (staleAssets.isEmpty()) {
            log.debug("No orphaned uploads found for cleanup");
            return;
        }

        log.info("Found {} orphaned uploads older than {} hours. Cleaning up...",
                staleAssets.size(), thresholdHours);

        int successCount = 0;
        int failureCount = 0;

        for (MediaAsset asset : staleAssets) {
            try {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(asset.getBucketName())
                            .key(asset.getFileKey())
                            .build());
                } catch (NoSuchKeyException e) {
                    log.debug("No S3 object for asset {} – skipping deletion", asset.getId());
                }

                asset.setStatus(MediaStatus.FAILED);
                repository.save(asset);
                successCount++;
                log.info("Marked orphaned upload {} as FAILED", asset.getId());
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to clean up orphaned upload {}: {}", asset.getId(), e.getMessage(), e);
            }
        }

        log.info("Orphan cleanup completed. Success: {}, Failures: {}", successCount, failureCount);
    }
}