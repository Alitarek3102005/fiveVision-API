package com.fivevision.api.catalog.internal.mapper;

import com.fivevision.api.catalog.internal.dto.CategoryResponse;
import com.fivevision.api.catalog.internal.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "subCategories", source = "subCategories")
    CategoryResponse toCategoryResponse(Category category);
}