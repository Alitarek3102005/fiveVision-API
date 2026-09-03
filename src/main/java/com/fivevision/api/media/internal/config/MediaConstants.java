package com.fivevision.api.media.internal.config;

import com.fivevision.api.media.internal.entity.MediaType;
import com.fivevision.api.common.exception.InvalidUploadException;

import java.util.Set;

public final class MediaConstants {

    private MediaConstants() {}

    public static final long MAX_PHOTO_SIZE_BYTES = 25L * 1024 * 1024;      // 25 MB
    public static final long MAX_VIDEO_SIZE_BYTES = 2L * 1024 * 1024 * 1024; // 2 GB
    public static final long MAX_THUMBNAIL_SIZE_BYTES = 5L * 1024 * 1024;    // 5 MB

    public static final int MIN_RESOLUTION = 1;
    public static final int MAX_RESOLUTION = 16384;  // 16K UHD

    public static final long MIN_DURATION_SECONDS = 0;
    public static final long MAX_DURATION_SECONDS = 86400;  // 24 hours

    public static final Set<String> ALLOWED_PHOTO_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
    public static final Set<String> ALLOWED_VIDEO_MIME_TYPES =
            Set.of("video/mp4", "video/quicktime", "video/webm");
    public static final Set<String> ALLOWED_THUMBNAIL_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    public static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("createdAt", "fileSizeBytes", "status", "type");



    public static long maxSizeFor(MediaType type) {
        return switch (type) {
            case PHOTO -> MAX_PHOTO_SIZE_BYTES;
            case VIDEO -> MAX_VIDEO_SIZE_BYTES;
            case THUMBNAIL -> MAX_THUMBNAIL_SIZE_BYTES;
        };
    }

    public static Set<String> allowedMimeTypesFor(MediaType type) {
        return switch (type) {
            case PHOTO -> ALLOWED_PHOTO_MIME_TYPES;
            case VIDEO -> ALLOWED_VIDEO_MIME_TYPES;
            case THUMBNAIL -> ALLOWED_THUMBNAIL_MIME_TYPES;
        };
    }


    public static void validateMetadata(Integer width, Integer height, Integer durationSeconds) {
        if (width != null) {
            if (width < MIN_RESOLUTION || width > MAX_RESOLUTION) {
                throw new InvalidUploadException(
                        "Resolution width must be between %d and %d pixels, got %d"
                                .formatted(MIN_RESOLUTION, MAX_RESOLUTION, width));
            }
        }

        if (height != null) {
            if (height < MIN_RESOLUTION || height > MAX_RESOLUTION) {
                throw new InvalidUploadException(
                        "Resolution height must be between %d and %d pixels, got %d"
                                .formatted(MIN_RESOLUTION, MAX_RESOLUTION, height));
            }
        }

        if (durationSeconds != null) {
            if (durationSeconds < MIN_DURATION_SECONDS || durationSeconds > MAX_DURATION_SECONDS) {
                throw new InvalidUploadException(
                        "Duration must be between %d and %d seconds, got %d"
                                .formatted(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS, durationSeconds));
            }
        }
    }


    public static void validateMimeType(MediaType mediaType, String mimeType) {
        if (!allowedMimeTypesFor(mediaType).contains(mimeType)) {
            throw new InvalidUploadException(
                    "MIME type '%s' is not allowed for %s media".formatted(mimeType, mediaType));
        }
    }


    public static void validateSize(MediaType mediaType, long sizeBytes) {
        long maxSize = maxSizeFor(mediaType);
        if (sizeBytes > maxSize) {
            throw new InvalidUploadException(
                    "File size %d bytes exceeds maximum of %d bytes for %s media"
                            .formatted(sizeBytes, maxSize, mediaType));
        }
    }


    public static void validateActualSize(long declaredBytes, long actualBytes) {
        if (declaredBytes != actualBytes) {
            throw new InvalidUploadException(
                    "Uploaded file size %d bytes does not match declared size %d bytes"
                            .formatted(actualBytes, declaredBytes));
        }
    }

    public static void validateContentType(String declaredMimeType, String actualContentType) {
        String declared = (declaredMimeType != null ? declaredMimeType : "").toLowerCase().trim();
        String actual = (actualContentType != null ? actualContentType : "").toLowerCase().trim();

        if (!declared.equals(actual)) {
            throw new InvalidUploadException(
                    "Uploaded file content-type '%s' does not match declared type '%s'"
                            .formatted(actual, declared));
        }
    }
}