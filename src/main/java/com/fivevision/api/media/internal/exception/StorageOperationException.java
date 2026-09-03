package com.fivevision.api.media.internal.exception;

public class StorageOperationException extends RuntimeException {
    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}