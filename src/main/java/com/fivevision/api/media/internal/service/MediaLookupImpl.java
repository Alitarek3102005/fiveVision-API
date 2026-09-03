package com.fivevision.api.media.internal.service;

import com.fivevision.api.media.MediaLookup;
import com.fivevision.api.media.MediaPublicSummary;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaLookupImpl implements MediaLookup {
    private final MediaAssetRepository mediaAssetRepository;

    @Override
    public Optional<MediaPublicSummary> findPublicSummary(UUID mediaId) {
        return mediaAssetRepository.findById(mediaId)
                .map(asset -> new MediaPublicSummary(asset.getId(), asset.getCdnUrl(), asset.getType().name()));
    }
}