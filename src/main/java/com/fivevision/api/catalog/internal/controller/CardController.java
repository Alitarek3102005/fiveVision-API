package com.fivevision.api.catalog.internal.controller;

import com.fivevision.api.catalog.internal.api.CardsApi;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.service.CardService;
import com.fivevision.api.common.exception.UnauthorizedAccessException;
import com.fivevision.api.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CardController implements CardsApi {

    private final CardService cardService;
    private static final int MAX_PAGE_SIZE = 100;
    private SecurityUtils securityUtils;

    @Override
    public ResponseEntity<PagedCardResponse> getCards(Integer page, Integer size, String sort, String search, UUID categoryId, UUID tagId, Boolean isPremium) {
        int safeSize = Math.min(size != null ? size : 20, MAX_PAGE_SIZE);
        return ResponseEntity.ok(cardService.getCards(page, safeSize, sort, search, categoryId, tagId, isPremium));
    }

    @Override
    public ResponseEntity<PagedCardResponse> getMyCards(Integer page, Integer size, String status, String sort) {
        UUID authorId = securityUtils.getCurrentUserId();
        int safeSize = Math.min(size != null ? size : 20, MAX_PAGE_SIZE);
        return ResponseEntity.ok(cardService.getMyCards(authorId, page, safeSize, status, sort));
    }

    @Override
    public ResponseEntity<CardDetailResponse> getCardById(UUID id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @Override
    public ResponseEntity<CardDetailResponse> createCard(CreateCardRequest request) {
        UUID authorId = securityUtils.getCurrentUserId();
        CardDetailResponse createdCard = cardService.createCard(request, authorId);
        return ResponseEntity.created(URI.create("/api/v1/cards/" + createdCard.getId())).body(createdCard);
    }

    @Override
    public ResponseEntity<CardDetailResponse> updateCard(UUID id, UpdateCardRequest request) {
        UUID requesterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cardService.updateCard(id, request, requesterId));
    }

    @Override
    public ResponseEntity<Void> deleteCard(UUID id) {
        UUID requesterId = securityUtils.getCurrentUserId();
        cardService.deleteCard(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> incrementCardView(UUID id) {
        cardService.incrementCardView(id);
        return ResponseEntity.noContent().build();
    }


}