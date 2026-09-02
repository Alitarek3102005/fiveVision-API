package com.fivevision.api.catalog.internal.mapper;

import com.fivevision.api.catalog.internal.dto.TagResponse;
import com.fivevision.api.catalog.internal.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {

    TagResponse toTagResponse(Tag tag);
}