package com.fivevision.api.media.internal.listener;

import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import com.fivevision.api.media.internal.entity.MediaType;
import com.fivevision.api.media.internal.event.MediaUploadCompletedEvent;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import com.fivevision.api.media.internal.service.ClamAvScanService;
import com.fivevision.api.media.internal.service.ImageProcessingService;
import com.fivevision.api.media.internal.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaScanListener {

    private final MediaAssetRepository repository;
    private final ClamAvScanService clamAvScanService;
    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploadCompleted(MediaUploadCompletedEvent event) {
        MediaAsset asset = repository.findById(event.mediaId())
                .orElseThrow(() -> new IllegalStateException(
                        "Media not found: " + event.mediaId()));

        try {
            boolean clean = clamAvScanService.scanObject(
                    asset.getBucketName(), asset.getFileKey());

            if (!clean) {
                log.warn("Media {} failed malware scan", asset.getId());
                asset.setStatus(MediaStatus.FAILED);
                repository.save(asset);
                return;
            }

            // ---- VARIANTS / POSTERS ----
            if (asset.getType() == MediaType.PHOTO || asset.getType() == MediaType.THUMBNAIL) {

                log.info("Generating variants for {}/{}",
                        asset.getBucketName(), asset.getFileKey());

                var variants = imageProcessingService.generateVariants(
                        asset.getBucketName(), asset.getFileKey());

                if (variants != null) {
                    asset.setThumbnailKey(variants.thumbnailKey());
                    asset.setThumbnailUrl(imageProcessingService.publicUrl(variants.thumbnailKey()));
                    asset.setLargeKey(variants.largeKey());
                    asset.setLargeUrl(imageProcessingService.publicUrl(variants.largeKey()));
                    log.info("Variants attached to asset {}: thumb={}, large={}",
                            asset.getId(), variants.thumbnailKey(), variants.largeKey());
                } else {
                    log.warn("generateVariants returned null for {}", asset.getId());
                }

            } else if (asset.getType() == MediaType.VIDEO) {

                log.info("Generating poster for video {}/{}",
                        asset.getBucketName(), asset.getFileKey());

                var poster = videoProcessingService.generatePoster(
                        asset.getBucketName(), asset.getFileKey());

                if (poster != null) {
                    asset.setThumbnailKey(poster.thumbnailKey());
                    asset.setThumbnailUrl(videoProcessingService.publicUrl(poster.thumbnailKey()));
                    // No largeUrl for videos — the mp4 itself is the large version.
                    log.info("Poster attached to video asset {}: thumb={}",
                            asset.getId(), poster.thumbnailKey());
                } else {
                    log.info("No poster generated for video {} (ffmpeg unavailable or failed); " +
                            "frontend will use the author-chosen poster if any", asset.getId());
                }
            }

            asset.setStatus(MediaStatus.READY);
            repository.save(asset);
            log.info("Media {} passed malware scan and is now READY", asset.getId());

        } catch (Exception ex) {
            log.error("Scan pipeline failed for media {}", asset.getId(), ex);
            asset.setStatus(MediaStatus.FAILED);
            repository.save(asset);
        }
    }
}