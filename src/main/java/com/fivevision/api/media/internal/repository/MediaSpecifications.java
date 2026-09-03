package com.fivevision.api.media.internal.repository;

import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import com.fivevision.api.media.internal.entity.MediaType;
import org.springframework.data.jpa.domain.Specification;

public final class MediaSpecifications {

    private MediaSpecifications() {}

    public static Specification<MediaAsset> hasType(MediaType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<MediaAsset> hasStatus(MediaStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<MediaAsset> filterBy(MediaType type, MediaStatus status) {
        return Specification.where(hasType(type)).and(hasStatus(status));
    }
}