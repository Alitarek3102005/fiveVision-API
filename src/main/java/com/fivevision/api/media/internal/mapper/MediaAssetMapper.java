package com.fivevision.api.media.internal.mapper;

import com.fivevision.api.media.internal.dto.MediaAssetResponse;
import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.net.URI;
import java.net.URISyntaxException;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaAssetMapper {

    @Mapping(target = "cdnUrl", expression = "java(entity.getStatus() == MediaStatus.READY ? toUri(entity.getCdnUrl()) : null)")
    MediaAssetResponse toResponse(MediaAsset entity);

    default URI toUri(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + value, e);
        }
    }
}