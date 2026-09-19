package com.fivevision.api.media;

import java.util.UUID;
public record MediaPublicSummary(
        UUID id,
        String cdnUrl,
        String thumbnailUrl,
        String largeUrl,
        String type,
        Integer durationSeconds
) {}