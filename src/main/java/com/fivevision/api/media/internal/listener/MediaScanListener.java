package com.fivevision.api.media.internal.listener;

import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import com.fivevision.api.media.internal.event.MediaUploadCompletedEvent;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import com.fivevision.api.media.internal.service.ClamAvScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaScanListener {

    private final ClamAvScanService clamAvScanService;
    private final MediaAssetRepository repository;
    private final S3Client s3Client;

    @Async
    @Transactional
    public void handleMediaUploadCompleted(MediaUploadCompletedEvent event) {
        UUID mediaId = event.mediaId();
        MediaAsset asset = repository.findById(mediaId).orElse(null);
        if (asset == null || asset.getStatus() != MediaStatus.SCANNING) {
            log.warn("Ignoring scan event for media {} (not found or not in SCANNING)", mediaId);
            return;
        }

        try {
            boolean clean = clamAvScanService.scanObject(asset.getBucketName(), asset.getFileKey());
            if (clean) {
                asset.setStatus(MediaStatus.READY);
                repository.save(asset);
                log.info("Media {} passed malware scan and is now READY", mediaId);
            } else {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(asset.getBucketName())
                            .key(asset.getFileKey())
                            .build());
                } catch (Exception e) {
                    log.error("Failed to delete infected object for media {}", mediaId, e);
                }
                asset.setStatus(MediaStatus.FAILED);
                repository.save(asset);
                log.warn("Media {} found infected and marked FAILED", mediaId);
            }
        } catch (Exception e) {
            log.error("Malware scan failed for media {}; leaving status as SCANNING", mediaId, e);
        }
    }

    @EventListener
    public void onMediaUploadCompleted(MediaUploadCompletedEvent event) {
        handleMediaUploadCompleted(event);
    }
}