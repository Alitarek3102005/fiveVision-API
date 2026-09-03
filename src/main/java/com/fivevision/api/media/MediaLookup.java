package com.fivevision.api.media;

import java.util.Optional;
import java.util.UUID;

public interface MediaLookup {
    Optional<MediaPublicSummary> findPublicSummary(UUID mediaId);
}