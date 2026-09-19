package com.fivevision.api.catalog.internal.service;

import com.fivevision.api.catalog.internal.dto.CardSummaryResponse;
import com.fivevision.api.catalog.internal.dto.MediaSummary;
import com.fivevision.api.catalog.internal.dto.PagedCardResponse;
import com.fivevision.api.catalog.internal.entity.Favorite;
import com.fivevision.api.catalog.internal.entity.FavoriteId;
import com.fivevision.api.catalog.internal.entity.NatureCard;
import com.fivevision.api.catalog.internal.mapper.CardMapper;
import com.fivevision.api.catalog.internal.repository.CardRepository;
import com.fivevision.api.catalog.internal.repository.FavoriteRepository;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import com.fivevision.api.media.MediaLookup;
import com.fivevision.api.media.MediaPublicSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FavoriteRepository favoriteRepository;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    private final MediaLookup mediaLookup;

    @Transactional(readOnly = true)
    public PagedCardResponse getFavoriteCards(UUID userId, Integer page, Integer size) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? size : 20;
        if (safePage < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (safeSize < 1 || safeSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NatureCard> favoritedCards = favoriteRepository.findFavoritedCardsByUserId(userId, pageRequest);

        PagedCardResponse response = cardMapper.toPagedResponse(favoritedCards);

        enrichSummariesWithMedia(response, favoritedCards);

        return response;
    }

    @Transactional
    public void favoriteCard(UUID cardId, UUID userId) {
        FavoriteId favoriteId = new FavoriteId(userId, cardId);

        if (favoriteRepository.existsById(favoriteId)) {
            log.debug("User [{}] already favorited card [{}]", userId, cardId);
            return;
        }

        NatureCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));

        Favorite favorite = Favorite.builder()
                .id(favoriteId)
                .card(card)
                .build();

        favoriteRepository.save(favorite);

        card.setFavoriteCount(card.getFavoriteCount() + 1);
        cardRepository.save(card);

        log.info("User [{}] successfully favorited card [{}]", userId, cardId);
    }

    @Transactional
    public void unfavoriteCard(UUID cardId, UUID userId) {
        FavoriteId favoriteId = new FavoriteId(userId, cardId);

        if (favoriteRepository.existsById(favoriteId)) {
            favoriteRepository.deleteById(favoriteId);

            NatureCard card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));

            card.setFavoriteCount(Math.max(0, card.getFavoriteCount() - 1));
            cardRepository.save(card);

            log.info("User [{}] successfully unfavorited card [{}]", userId, cardId);
        }
    }

    private void enrichSummariesWithMedia(PagedCardResponse response, Page<NatureCard> cardPage) {
        if (response.getContent() == null) return;

        for (CardSummaryResponse summary : response.getContent()) {
            cardPage.stream()
                    .filter(c -> c.getId().equals(summary.getId()))
                    .findFirst()
                    .ifPresent(card -> {
                        if (card.getThumbnailMediaId() != null) {
                            mediaLookup.findPublicSummary(card.getThumbnailMediaId())
                                    .ifPresent(media -> summary.setThumbnailMedia(toMediaSummary(media)));
                        }
                    });
        }
    }

    private MediaSummary toMediaSummary(MediaPublicSummary media) {
        return new MediaSummary()
                .id(media.id())
                .cdnUrl(media.cdnUrl())
                .thumbnailUrl(media.thumbnailUrl())
                .largeUrl(media.largeUrl())
                .type(MediaSummary.TypeEnum.fromValue(media.type()));
    }
}