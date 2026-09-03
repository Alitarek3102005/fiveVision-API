package com.fivevision.api.identity.internal.service;


import com.fivevision.api.identity.internal.dto.PagedUserResponse;
import com.fivevision.api.identity.internal.dto.UpdateProfileRequest;
import com.fivevision.api.identity.internal.dto.UserProfileResponse;
import com.fivevision.api.identity.internal.dto.UserSummaryResponse;

import java.util.UUID;

public interface UserService {

    PagedUserResponse listUsers(int page, int size, String sort, String search);

    UserProfileResponse getCurrentUser();

    UserProfileResponse updateCurrentUser(UpdateProfileRequest request);

    UserProfileResponse syncUser();

    UserSummaryResponse getUserSummary(UUID id);
}