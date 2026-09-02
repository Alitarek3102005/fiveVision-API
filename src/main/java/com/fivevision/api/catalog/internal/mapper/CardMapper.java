package com.fivevision.api.catalog.internal.mapper;

import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class, TagMapper.class}
)
public interface CardMapper {

    @Mapping(target = "thumbnailMedia", ignore = true)
    CardSummaryResponse toSummaryResponse(NatureCard card);

    @Mapping(target = "primaryMedia", ignore = true)
    @Mapping(target = "thumbnailMedia", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "isFavorited", constant = "false")
    CardDetailResponse toDetailResponse(NatureCard card);

    default PagedCardResponse toPagedResponse(Page<NatureCard> page) {
        if (page == null) {
            return null;
        }

        List<CardSummaryResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new PagedCardResponse()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast());
    }
}