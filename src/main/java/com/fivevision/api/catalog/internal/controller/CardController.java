package com.fivevision.api.catalog.internal.controller;

import com.fivevision.api.catalog.internal.api.CardsApi;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.service.CardService;
import com.fivevision.api.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CardController implements CardsApi {

    private final CardService cardService;
    private final SecurityUtils securityUtils;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<PagedCardResponse> getCards(Integer page, Integer size, String sort, String search,
                                                      UUID categoryId, UUID tagId, Boolean isPremium) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? Math.min(size, MAX_PAGE_SIZE) : 20;
        return ResponseEntity.ok(cardService.getCards(safePage, safeSize, sort, search, categoryId, tagId, isPremium));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedCardResponse> getMyCards(Integer page, Integer size, String status, String sort) {
        UUID authorId = securityUtils.getCurrentUserId();
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? Math.min(size, MAX_PAGE_SIZE) : 20;
        return ResponseEntity.ok(cardService.getMyCards(authorId, safePage, safeSize, status, sort));
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<CardDetailResponse> getCardById(UUID id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<CardDetailResponse> createCard(CreateCardRequest request) {
        UUID authorId = securityUtils.getCurrentUserId();
        CardDetailResponse createdCard = cardService.createCard(request, authorId);
        return ResponseEntity
                .created(URI.create("/api/v1/cards/" + createdCard.getId()))
                .body(createdCard);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CardDetailResponse> updateCard(UUID id, UpdateCardRequest request) {
        UUID requesterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cardService.updateCard(id, request, requesterId));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCard(UUID id) {
        UUID requesterId = securityUtils.getCurrentUserId();
        cardService.deleteCard(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> incrementCardView(UUID id) {
        cardService.incrementCardView(id);
        return ResponseEntity.noContent().build();
    }
}