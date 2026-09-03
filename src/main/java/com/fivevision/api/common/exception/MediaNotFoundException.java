package com.fivevision.api.common.exception;

import java.util.UUID;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(UUID id) {
        super("Media asset not found: " + id);
    }
}