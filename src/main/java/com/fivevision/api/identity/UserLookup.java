package com.fivevision.api.identity;

import java.util.Optional;
import java.util.UUID;

public interface UserLookup {
    Optional<UserPublicSummary> findPublicSummary(UUID userId);
}