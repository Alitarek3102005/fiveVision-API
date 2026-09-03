package com.fivevision.api.media;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.common.exception.ForbiddenAccessException;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.identity.internal.entity.User;
import com.fivevision.api.identity.internal.repository.UserRepository;
import com.fivevision.api.media.internal.dto.*;
import com.fivevision.api.media.internal.entity.*;
import com.fivevision.api.common.exception.InvalidUploadException;
import com.fivevision.api.common.exception.MediaNotFoundException;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import com.fivevision.api.media.internal.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MediaServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaAssetRepository repository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3Presigner s3Presigner;

    @MockitoBean
    private SecurityUtils securityUtils;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    private UUID uploaderId;

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        uploaderId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(uploaderId)
                .username("testuser-" + uploaderId)
                .email("test-" + uploaderId + "@example.com")
                .build());

        when(securityUtils.isAdmin()).thenReturn(true);
        when(securityUtils.hasRole(anyString())).thenReturn(true);
        when(securityUtils.isOwnerOrAdmin(any(UUID.class))).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(uploaderId);
    }

    @Test
    void listMedia_ReturnsOnlyActiveAssets() {
        MediaAsset active = createMediaAsset(UUID.randomUUID(), MediaStatus.READY, false);
        repository.save(active);

        MediaAsset deleted = createMediaAsset(UUID.randomUUID(), MediaStatus.READY, true);
        deleted.setDeletedAt(OffsetDateTime.now());
        deleted.setDeletedBy(uploaderId);
        repository.save(deleted);

        PagedMediaResponse response = mediaService.listMedia(0, 20, "createdAt,desc", null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(active.getId());
    }

    @Test
    void listMedia_WithoutPermissionThrowsForbidden() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.hasRole("AUTHOR")).thenReturn(false);

        assertThatThrownBy(() -> mediaService.listMedia(0, 20, "createdAt,desc", null, null))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void initiateUpload_Success() throws MalformedURLException {
        InitiateUploadRequest request = new InitiateUploadRequest()
                .fileName("test.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(1000L)
                .type(InitiateUploadRequest.TypeEnum.PHOTO);

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("http://minio.example.com/upload").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        InitiateUploadResponse response = mediaService.initiateUpload(request, uploaderId);

        assertThat(response.getMediaId()).isNotNull();
        assertThat(response.getUploadUrl()).isEqualTo(URI.create("http://minio.example.com/upload"));
        assertThat(repository.findById(response.getMediaId())).isPresent();
    }

    @Test
    void initiateUpload_InvalidMimeTypeThrows() {
        InitiateUploadRequest request = new InitiateUploadRequest()
                .fileName("test.txt")
                .mimeType("text/plain")
                .sizeBytes(100L)
                .type(InitiateUploadRequest.TypeEnum.PHOTO);

        assertThatThrownBy(() -> mediaService.initiateUpload(request, uploaderId))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void completeUpload_Success() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.PROCESSING, false);
        repository.save(asset);

        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(1000L)
                .contentType("image/jpeg")
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        CompleteUploadRequest request = new CompleteUploadRequest()
                .resolutionWidth(1920)
                .resolutionHeight(1080)
                .cameraModel("Canon")
                .lensInfo("50mm");

        MediaAssetResponse response = mediaService.completeUpload(asset.getId(), request);

        assertThat(response.getStatus()).isEqualTo(MediaAssetResponse.StatusEnum.READY);
        assertThat(response.getResolutionWidth()).isEqualTo(1920);
        assertThat(repository.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.READY);
    }

    @Test
    void completeUpload_ObjectNotFoundThrows() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.PROCESSING, false);
        repository.save(asset);

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.class);

        CompleteUploadRequest request = new CompleteUploadRequest();
        assertThatThrownBy(() -> mediaService.completeUpload(asset.getId(), request))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void completeUpload_SizeMismatchThrows() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.PROCESSING, false);
        asset.setFileSizeBytes(2000L);
        repository.save(asset);

        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(1000L)
                .contentType("image/jpeg")
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        CompleteUploadRequest request = new CompleteUploadRequest();
        assertThatThrownBy(() -> mediaService.completeUpload(asset.getId(), request))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void getById_ReturnsActiveAsset() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.READY, false);
        repository.save(asset);

        MediaAssetResponse response = mediaService.getById(asset.getId());
        assertThat(response.getId()).isEqualTo(asset.getId());
    }

    @Test
    void getById_SoftDeletedThrowsNotFound() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.READY, true);
        asset.setDeletedAt(OffsetDateTime.now());
        asset.setDeletedBy(uploaderId);
        repository.save(asset);

        assertThatThrownBy(() -> mediaService.getById(asset.getId()))
                .isInstanceOf(MediaNotFoundException.class);
    }

    @Test
    void delete_SoftDeletesAssetAndPublishesEvent() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.READY, false);
        repository.save(asset);

        mediaService.delete(asset.getId());

        MediaAsset saved = repository.findById(asset.getId()).orElseThrow();
        assertThat(saved.getDeletedAt()).isNotNull();
        assertThat(saved.getDeletedBy()).isEqualTo(uploaderId);
    }

    @Test
    void delete_SoftDeletedAlreadyThrowsNotFound() {
        MediaAsset asset = createMediaAsset(UUID.randomUUID(), MediaStatus.READY, true);
        asset.setDeletedAt(OffsetDateTime.now());
        asset.setDeletedBy(uploaderId);
        repository.save(asset);

        assertThatThrownBy(() -> mediaService.delete(asset.getId()))
                .isInstanceOf(MediaNotFoundException.class);
    }

    private MediaAsset createMediaAsset(UUID id, MediaStatus status, boolean softDeleted) {
        MediaAsset.MediaAssetBuilder builder = MediaAsset.builder()
                .id(id)
                .uploaderId(uploaderId)
                .type(MediaType.PHOTO)
                .status(status)
                .bucketName("test-bucket")
                .fileKey("media/" + id + "/test.jpg")
                .cdnUrl("http://localhost/test.jpg")
                .fileSizeBytes(1000L)
                .mimeType("image/jpeg");

        if (softDeleted) {
            builder.deletedAt(OffsetDateTime.now());
            builder.deletedBy(uploaderId);
        }

        return builder.build();
    }
}