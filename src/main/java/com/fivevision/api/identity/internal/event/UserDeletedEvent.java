package com.fivevision.api.identity.internal.event;

import java.util.UUID;

public record UserDeletedEvent(UUID userId) {
}