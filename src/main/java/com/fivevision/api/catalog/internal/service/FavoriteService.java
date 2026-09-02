package com.fivevision.api.catalog.internal.service;

import com.fivevision.api.catalog.internal.dto.PagedCardResponse;
import com.fivevision.api.catalog.internal.entity.Favorite;
import com.fivevision.api.catalog.internal.entity.FavoriteId;
import com.fivevision.api.catalog.internal.entity.NatureCard;
import com.fivevision.api.catalog.internal.mapper.CardMapper;
import com.fivevision.api.catalog.internal.repository.CardRepository;
import com.fivevision.api.catalog.internal.repository.FavoriteRepository;
import com.fivevision.api.common.exception.ResourceNotFoundException;
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

    private final FavoriteRepository favoriteRepository;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    @Transactional(readOnly = true)
    public PagedCardResponse getFavoriteCards(UUID userId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NatureCard> favoritedCards = favoriteRepository.findFavoritedCardsByUserId(userId, pageRequest);
        return cardMapper.toPagedResponse(favoritedCards);
    }

    @Transactional
    public void favoriteCard(UUID cardId, UUID userId) {
        if (favoriteRepository.existsByIdUserIdAndIdCardId(userId, cardId)) {
            log.debug("User [{}] already favorited card [{}]", userId, cardId);
            return;
        }

        NatureCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));

        Favorite favorite = Favorite.builder()
                .id(new FavoriteId(userId, cardId))
                .card(card)
                .build();

        favoriteRepository.save(favorite);

        card.setFavoriteCount(card.getFavoriteCount() + 1);
        cardRepository.save(card);

        log.info("User [{}] successfully favorited card [{}]", userId, cardId);
    }

    @Transactional
    public void unfavoriteCard(UUID cardId, UUID userId) {
        FavoriteId id = new FavoriteId(userId, cardId);

        if (favoriteRepository.existsById(id)) {
            favoriteRepository.deleteById(id);

            NatureCard card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));

            card.setFavoriteCount(Math.max(0, card.getFavoriteCount() - 1));
            cardRepository.save(card);

            log.info("User [{}] successfully unfavorited card [{}]", userId, cardId);
        }
    }
}