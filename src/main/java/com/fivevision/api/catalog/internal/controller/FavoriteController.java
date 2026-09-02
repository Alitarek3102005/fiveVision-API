package com.fivevision.api.catalog.internal.controller;

import com.fivevision.api.catalog.internal.api.FavoritesApi;
import com.fivevision.api.catalog.internal.dto.PagedCardResponse;
import com.fivevision.api.catalog.internal.service.FavoriteService;
import com.fivevision.api.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FavoriteController implements FavoritesApi {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public ResponseEntity<PagedCardResponse> getFavoriteCards(Integer page, Integer size) {
        UUID userId = securityUtils.getCurrentUserId();
        int safeSize = Math.min(size != null ? size : 20, MAX_PAGE_SIZE);

        return ResponseEntity.ok(favoriteService.getFavoriteCards(userId, page, safeSize));
    }

    @Override
    public ResponseEntity<Void> favoriteCard(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        favoriteService.favoriteCard(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> unfavoriteCard(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        favoriteService.unfavoriteCard(id, userId);
        return ResponseEntity.noContent().build();
    }
}