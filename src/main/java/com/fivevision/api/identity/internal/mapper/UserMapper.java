package com.fivevision.api.identity.internal.mapper;


import com.fivevision.api.identity.internal.dto.UserProfileResponse;
import com.fivevision.api.identity.internal.dto.UserSummaryResponse;
import com.fivevision.api.identity.internal.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserProfileResponse toProfileResponse(User user);

    UserSummaryResponse toSummaryResponse(User user);
}