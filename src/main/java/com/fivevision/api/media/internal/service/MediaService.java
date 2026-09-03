package com.fivevision.api.media.internal.service;

import com.fivevision.api.media.internal.dto.*;
import java.util.UUID;

public interface MediaService {
    PagedMediaResponse listMedia(int page, int size, String sort, String type, String status);
    InitiateUploadResponse initiateUpload(InitiateUploadRequest request, UUID uploaderId);
    MediaAssetResponse completeUpload(UUID id, CompleteUploadRequest request);
    MediaAssetResponse getById(UUID id);
    void delete(UUID id);
}