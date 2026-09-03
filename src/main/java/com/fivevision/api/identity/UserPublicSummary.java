package com.fivevision.api.identity;

import java.util.UUID;

public record UserPublicSummary(
        UUID id,
        String username,
        String firstName,
        String lastName
) {}