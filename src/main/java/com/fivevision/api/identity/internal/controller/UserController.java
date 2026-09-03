package com.fivevision.api.identity.internal.controller;

import com.fivevision.api.identity.internal.api.UsersApi;
import com.fivevision.api.identity.internal.dto.*;
import com.fivevision.api.identity.internal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final UserService userService;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedUserResponse> getUsers(Integer page, Integer size, String search, String sort) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? Math.min(size, MAX_PAGE_SIZE) : 20;
        String safeSort = sort != null ? sort : "createdAt,desc";
        return ResponseEntity.ok(userService.listUsers(safePage, safeSize, safeSort, search));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @Valid UpdateProfileRequest updateProfileRequest) {
        return ResponseEntity.ok(userService.updateCurrentUser(updateProfileRequest));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> syncUser() {
        return ResponseEntity.ok(userService.syncUser());
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserSummaryResponse> getUserById(UUID id) {
        return ResponseEntity.ok(userService.getUserSummary(id));
    }
}