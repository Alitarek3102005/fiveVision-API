package com.fivevision.api.catalog.internal.controller;

import com.fivevision.api.catalog.internal.api.FavoritesApi;
import com.fivevision.api.catalog.internal.dto.PagedCardResponse;
import com.fivevision.api.catalog.internal.service.FavoriteService;
import com.fivevision.api.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FavoriteController implements FavoritesApi {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedCardResponse> getFavoriteCards(Integer page, Integer size) {
        UUID userId = securityUtils.getCurrentUserId();
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? Math.min(size, MAX_PAGE_SIZE) : 20;
        return ResponseEntity.ok(favoriteService.getFavoriteCards(userId, safePage, safeSize));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> favoriteCard(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        favoriteService.favoriteCard(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unfavoriteCard(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        favoriteService.unfavoriteCard(id, userId);
        return ResponseEntity.noContent().build();
    }
}