package com.fivevision.api.catalog;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.catalog.internal.dto.PagedCardResponse;
import com.fivevision.api.catalog.internal.entity.Favorite;
import com.fivevision.api.catalog.internal.entity.FavoriteId;
import com.fivevision.api.catalog.internal.entity.NatureCard;
import com.fivevision.api.catalog.internal.repository.CardRepository;
import com.fivevision.api.catalog.internal.repository.FavoriteRepository;
import com.fivevision.api.catalog.internal.service.FavoriteService;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import com.fivevision.api.identity.internal.entity.User;
import com.fivevision.api.identity.internal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class FavoriteServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authorId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(authorId)
                .username("author-" + authorId)
                .email("author-" + authorId + "@example.com")
                .build());

        userRepository.save(User.builder()
                .id(userId)
                .username("user-" + userId)
                .email("user-" + userId + "@example.com")
                .build());
    }

    @Test
    void favoriteCard_ShouldCreateFavoriteAndIncrementCount() {
        NatureCard card = createCard(UUID.randomUUID(), 0L);
        cardRepository.save(card);

        favoriteService.favoriteCard(card.getId(), userId);

        assertThat(favoriteRepository.existsById(new FavoriteId(userId, card.getId()))).isTrue();
        NatureCard updated = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updated.getFavoriteCount()).isEqualTo(1);
    }

    @Test
    void favoriteCard_AlreadyFavorited_ShouldNotDuplicate() {
        NatureCard card = createCard(UUID.randomUUID(), 1L);
        cardRepository.save(card);
        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(userId, card.getId()))
                .card(card)
                .build());

        favoriteService.favoriteCard(card.getId(), userId);

        assertThat(favoriteRepository.count()).isEqualTo(1);
        assertThat(cardRepository.findById(card.getId()).orElseThrow().getFavoriteCount()).isEqualTo(1);
    }

    @Test
    void favoriteCard_CardNotFound_ThrowsResourceNotFound() {
        UUID nonExistentCardId = UUID.randomUUID();
        assertThatThrownBy(() -> favoriteService.favoriteCard(nonExistentCardId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void unfavoriteCard_ShouldRemoveFavoriteAndDecrementCount() {
        NatureCard card = createCard(UUID.randomUUID(), 1L);
        cardRepository.save(card);
        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(userId, card.getId()))
                .card(card)
                .build());

        favoriteService.unfavoriteCard(card.getId(), userId);

        assertThat(favoriteRepository.existsById(new FavoriteId(userId, card.getId()))).isFalse();
        NatureCard updated = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updated.getFavoriteCount()).isZero();
    }

    @Test
    void unfavoriteCard_NotFavorited_ShouldDoNothing() {
        NatureCard card = createCard(UUID.randomUUID(), 5L);
        cardRepository.save(card);

        favoriteService.unfavoriteCard(card.getId(), userId);

        assertThat(favoriteRepository.existsById(new FavoriteId(userId, card.getId()))).isFalse();
        assertThat(cardRepository.findById(card.getId()).orElseThrow().getFavoriteCount()).isEqualTo(5);
    }

    @Test
    void unfavoriteCard_NoFavorite_DoesNotThrow() {
        NatureCard card = createCard(UUID.randomUUID(), 5L);
        cardRepository.save(card);

        favoriteService.unfavoriteCard(card.getId(), userId);

        assertThat(favoriteRepository.existsById(new FavoriteId(userId, card.getId()))).isFalse();
    }

    @Test
    void getFavoriteCards_ReturnsPageOfFavoritedCards() {
        NatureCard favoritedCard = createCard(UUID.randomUUID(), 1L);
        NatureCard otherCard = createCard(UUID.randomUUID(), 0L);
        cardRepository.save(favoritedCard);
        cardRepository.save(otherCard);

        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(userId, favoritedCard.getId()))
                .card(favoritedCard)
                .build());

        PagedCardResponse response = favoriteService.getFavoriteCards(userId, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(favoritedCard.getId());
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getFavoriteCards_InvalidPage_Throws() {
        assertThatThrownBy(() -> favoriteService.getFavoriteCards(userId, -1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page must be >= 0");
    }

    @Test
    void getFavoriteCards_InvalidSize_Throws() {
        assertThatThrownBy(() -> favoriteService.getFavoriteCards(userId, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    private NatureCard createCard(UUID id, Long favoriteCount) {
        return NatureCard.builder()
                .id(id)
                .authorId(authorId)
                .title("Test Card")
                .speciesScientificName("Test species")
                .status(com.fivevision.api.catalog.internal.entity.CardStatus.PUBLISHED)
                .viewCount(0L)
                .favoriteCount(favoriteCount)
                .build();
    }
}