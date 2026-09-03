package com.fivevision.api.media.internal.controller;

import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.media.internal.api.MediaApi;
import com.fivevision.api.media.internal.dto.*;
import com.fivevision.api.media.internal.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MediaController implements MediaApi {

    private final MediaService mediaService;
    private final SecurityUtils securityUtils;

    @Override
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<PagedMediaResponse> getMediaAssets(
            Integer page, Integer size, String sort, String type, String status) {
        return ResponseEntity.ok(mediaService.listMedia(
                page != null ? page : 0,
                size != null ? size : 20,
                sort != null ? sort : "createdAt,desc",
                type, status));
    }

    @Override
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(InitiateUploadRequest initiateUploadRequest) {
        UUID uploaderId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(mediaService.initiateUpload(initiateUploadRequest, uploaderId));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaAssetResponse> completeUpload(UUID id, CompleteUploadRequest completeUploadRequest) {
        return ResponseEntity.ok(mediaService.completeUpload(id, completeUploadRequest));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaAssetResponse> getMediaById(UUID id) {
        return ResponseEntity.ok(mediaService.getById(id));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMedia(UUID id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}