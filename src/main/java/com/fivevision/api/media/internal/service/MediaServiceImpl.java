package com.fivevision.api.media.internal.service;

import com.fivevision.api.common.exception.ForbiddenAccessException;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.media.internal.config.MediaConstants;
import com.fivevision.api.media.internal.config.StorageProperties;
import com.fivevision.api.media.internal.dto.*;
import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import com.fivevision.api.media.internal.entity.MediaType;
import com.fivevision.api.media.internal.event.MediaAssetDeletedEvent;
import com.fivevision.api.media.internal.event.MediaUploadCompletedEvent;
import com.fivevision.api.common.exception.InvalidUploadException;
import com.fivevision.api.common.exception.MediaNotFoundException;
import com.fivevision.api.common.exception.StorageOperationException;
import com.fivevision.api.media.internal.mapper.MediaAssetMapper;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import com.fivevision.api.media.internal.repository.MediaSpecifications;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final int MAX_PAGE_SIZE = 100;
    private static final String AUTHOR_ROLE = "AUTHOR";

    private final MediaAssetRepository repository;
    private final MediaAssetMapper mapper;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PagedMediaResponse listMedia(int page, int size, String sort, String type, String status) {
        if (!securityUtils.isAdmin() && !securityUtils.hasRole(AUTHOR_ROLE)) {
            throw new ForbiddenAccessException("Only admins or authors can list media assets");
        }
        if (page < 0) throw new InvalidUploadException("page must be >= 0");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidUploadException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        MediaType mediaType = parseEnumOrNull(MediaType.class, type, "type");
        MediaStatus mediaStatus = parseEnumOrNull(MediaStatus.class, status, "status");

        Specification<MediaAsset> spec = Specification
                .where(MediaSpecifications.filterBy(mediaType, mediaStatus))
                .and(notDeletedSpec());

        Page<MediaAsset> result = repository.findAll(spec, PageRequest.of(page, size, parseSort(sort)));

        List<MediaAssetResponse> content = result.getContent().stream()
                .map(mapper::toResponse)
                .toList();

        PagedMediaResponse response = new PagedMediaResponse();
        response.setContent(content);
        response.setPageNumber(result.getNumber());
        response.setPageSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setIsLast(result.isLast());
        return response;
    }

    @Override
    @Transactional
    public InitiateUploadResponse initiateUpload(InitiateUploadRequest request, UUID uploaderId) {
        MediaType mediaType = parseEnumOrNull(MediaType.class, request.getType().getValue(), "type");
        if (mediaType == null) {
            throw new InvalidUploadException("type is required");
        }
        validateUploadRequest(request, mediaType);

        UUID mediaId = UUID.randomUUID();
        String fileKey = buildFileKey(mediaId, request.getFileName());

        MediaAsset asset = MediaAsset.builder()
                .id(mediaId)
                .uploaderId(uploaderId)
                .type(mediaType)
                .status(MediaStatus.PROCESSING)
                .bucketName(storageProperties.getBucketName())
                .fileKey(fileKey)
                .cdnUrl(buildCdnUrl(fileKey))
                .fileSizeBytes(request.getSizeBytes())
                .mimeType(request.getMimeType())
                .build();

        repository.save(asset);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .key(fileKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(UPLOAD_URL_TTL)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

            InitiateUploadResponse response = new InitiateUploadResponse();
            response.setMediaId(mediaId);
            response.setUploadUrl(URI.create(presigned.url().toString()));
            response.setFileKey(fileKey);
            response.setExpiresAt(OffsetDateTime.now().plus(UPLOAD_URL_TTL));

            log.info("Initiated upload for mediaId={}, uploaderId={}, type={}, size={}",
                    mediaId, uploaderId, mediaType, request.getSizeBytes());

            return response;
        } catch (SdkClientException | AwsServiceException ex) {
            log.error("Failed to generate presigned URL for mediaId={}", mediaId, ex);
            throw new StorageOperationException("Failed to generate upload URL", ex);
        }
    }

    @Override
    @Transactional
    public MediaAssetResponse completeUpload(UUID id, CompleteUploadRequest request) {
        MediaAsset asset = findActiveById(id);

        if (!securityUtils.isOwnerOrAdmin(asset.getUploaderId())) {
            throw new ForbiddenAccessException("You do not have permission to modify this media asset");
        }

        if (asset.getStatus() == MediaStatus.READY) {
            log.info("Asset {} already READY; returning current state", id);
            return mapper.toResponse(asset);
        }

        HeadObjectResponse headResponse;
        try {
            headResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(asset.getBucketName())
                    .key(asset.getFileKey())
                    .build());
        } catch (NoSuchKeyException e) {
            log.warn("No S3 object found for mediaId={} during completion", id);
            throw new InvalidUploadException(
                    "Uploaded object not found in storage. Please upload the file before completing.");
        } catch (SdkClientException | AwsServiceException e) {
            log.error("Failed to verify S3 object for mediaId={}", id, e);
            throw new StorageOperationException("Failed to verify uploaded object", e);
        }

        if (asset.getFileSizeBytes() != null) {
            MediaConstants.validateActualSize(asset.getFileSizeBytes(), headResponse.contentLength());
        }

        MediaConstants.validateContentType(asset.getMimeType(), headResponse.contentType());

        MediaConstants.validateMetadata(
                request.getResolutionWidth(),
                request.getResolutionHeight(),
                request.getDurationSeconds()
        );

        asset.setResolutionWidth(request.getResolutionWidth());
        asset.setResolutionHeight(request.getResolutionHeight());
        asset.setDurationSeconds(request.getDurationSeconds());
        asset.setCameraModel(request.getCameraModel());
        asset.setLensInfo(request.getLensInfo());

        asset.setStatus(MediaStatus.SCANNING);

        MediaAsset saved = repository.save(asset);
        log.info("Scheduled malware scan for mediaId={}, uploaderId={}", saved.getId(), saved.getUploaderId());

        eventPublisher.publishEvent(new MediaUploadCompletedEvent(saved.getId()));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAssetResponse getById(UUID id) {
        MediaAsset asset = findActiveById(id);

        if (!securityUtils.isOwnerOrAdmin(asset.getUploaderId())) {
            throw new ForbiddenAccessException("You do not have permission to view this media asset");
        }

        return mapper.toResponse(asset);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        MediaAsset asset = findActiveById(id);

        if (!securityUtils.isOwnerOrAdmin(asset.getUploaderId())) {
            throw new ForbiddenAccessException("You do not have permission to delete this media asset");
        }

        asset.setDeletedAt(OffsetDateTime.now());
        asset.setDeletedBy(securityUtils.getCurrentUserId());
        repository.save(asset);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(asset.getBucketName())
                    .key(asset.getFileKey())
                    .build());
        } catch (SdkClientException | AwsServiceException ex) {
            log.error("Failed to delete S3 object for mediaId={}", id, ex);
        }

        log.info("Soft-deleted media asset id={}, uploaderId={}", id, asset.getUploaderId());

        eventPublisher.publishEvent(new MediaAssetDeletedEvent(id));
    }

    private MediaAsset findActiveById(UUID id) {
        MediaAsset asset = repository.findById(id)
                .orElseThrow(() -> new MediaNotFoundException(id));
        if (asset.getDeletedAt() != null) {
            throw new MediaNotFoundException(id);
        }
        return asset;
    }

    private Specification<MediaAsset> notDeletedSpec() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private void validateUploadRequest(InitiateUploadRequest request, MediaType type) {
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new InvalidUploadException("fileName is required");
        }
        if (request.getSizeBytes() == null || request.getSizeBytes() <= 0) {
            throw new InvalidUploadException("sizeBytes must be positive");
        }
        if (request.getSizeBytes() > MediaConstants.maxSizeFor(type)) {
            throw new InvalidUploadException(
                    "File exceeds max size of %d bytes for type %s".formatted(MediaConstants.maxSizeFor(type), type));
        }
        if (request.getMimeType() == null
                || !MediaConstants.allowedMimeTypesFor(type).contains(request.getMimeType().toLowerCase())) {
            throw new InvalidUploadException("mimeType %s not allowed for type %s".formatted(request.getMimeType(), type));
        }

        validateFileExtension(request.getFileName(), request.getMimeType());
    }

    private void validateFileExtension(String fileName, String mimeType) {
        if (fileName == null || mimeType == null) return;

        String lowerName = fileName.toLowerCase();
        boolean valid = switch (mimeType.toLowerCase()) {
            case "image/jpeg" -> lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
            case "image/png" -> lowerName.endsWith(".png");
            case "image/webp" -> lowerName.endsWith(".webp");
            case "image/heic" -> lowerName.endsWith(".heic");
            case "video/mp4" -> lowerName.endsWith(".mp4");
            case "video/quicktime" -> lowerName.endsWith(".mov");
            case "video/webm" -> lowerName.endsWith(".webm");
            default -> true;
        };

        if (!valid) {
            throw new InvalidUploadException(
                    "File extension does not match declared MIME type: " + mimeType);
        }
    }

    private <E extends Enum<E>> E parseEnumOrNull(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUploadException("Invalid %s: %s".formatted(fieldName, value));
        }
    }

    private String buildFileKey(UUID mediaId, String fileName) {
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.length() > 200) {
            safeName = safeName.substring(safeName.length() - 200);
        }
        return "media/%s/%s".formatted(mediaId, safeName);
    }

    private String buildCdnUrl(String fileKey) {
        return "%s/%s/%s".formatted(storageProperties.getEndpointUrl(), storageProperties.getBucketName(), fileKey);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String property = parts[0];
        if (!MediaConstants.ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new InvalidUploadException("sort property not allowed: " + property);
        }
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}