package com.fivevision.api.catalog;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.entity.*;
import com.fivevision.api.catalog.internal.repository.CardRepository;
import com.fivevision.api.catalog.internal.repository.CategoryRepository;
import com.fivevision.api.catalog.internal.repository.FavoriteRepository;
import com.fivevision.api.catalog.internal.repository.TagRepository;
import com.fivevision.api.catalog.internal.service.CardService;
import com.fivevision.api.common.exception.ForbiddenAccessException;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.identity.UserLookup;
import com.fivevision.api.identity.UserPublicSummary;
import com.fivevision.api.identity.internal.entity.User;
import com.fivevision.api.identity.internal.repository.UserRepository;
import com.fivevision.api.media.MediaLookup;
import com.fivevision.api.media.MediaPublicSummary;
import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.media.internal.entity.MediaStatus;
import com.fivevision.api.media.internal.entity.MediaType;
import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Transactional
public class CardServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CardService cardService;
    @Autowired private CardRepository cardRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;

    @MockitoBean private UserLookup userLookup;
    @MockitoBean private MediaLookup mediaLookup;
    @MockitoBean private SecurityUtils securityUtils;

    private UUID authorId;
    private UUID mediaId;
    private UUID categoryId;
    private UUID tagId;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        mediaAssetRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        authorId = UUID.randomUUID();
        mediaId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        tagId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(authorId)
                .username("author-" + authorId)
                .email("author-" + authorId + "@example.com")
                .build());

        insertMediaRow(mediaId, MediaType.PHOTO, "image/jpeg", null);

        when(securityUtils.isOwnerOrAdmin(any(UUID.class))).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(authorId);
        when(securityUtils.isAdmin()).thenReturn(true);

        when(mediaLookup.findPublicSummary(mediaId))
                .thenReturn(Optional.of(photoSummary(mediaId)));

        when(userLookup.findPublicSummary(authorId))
                .thenReturn(Optional.of(new UserPublicSummary(authorId, "authoruser", "John", "Doe")));
    }


    @Test
    void getCards_ReturnsOnlyPublishedCards() {
        NatureCard publishedCard = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        NatureCard draftCard = createCard(UUID.randomUUID(), CardStatus.DRAFT, null, null);
        cardRepository.saveAll(List.of(publishedCard, draftCard));

        PagedCardResponse response =
                cardService.getCards(0, 20, "createdAt,desc", null, null, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(publishedCard.getId());
    }

    @Test
    void getCards_FilterByCategoryAndTag() {
        Category category = Category.builder().id(categoryId).name("Birds").slug("birds").build();
        Tag tag = Tag.builder().id(tagId).name("Rare").slug("rare").build();
        categoryRepository.save(category);
        tagRepository.save(tag);

        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED,
                Set.of(categoryId), Set.of(tagId));
        cardRepository.save(card);

        NatureCard otherCard = createCard(UUID.randomUUID(), CardStatus.PUBLISHED,
                new HashSet<>(), new HashSet<>());
        cardRepository.save(otherCard);

        PagedCardResponse response =
                cardService.getCards(0, 20, "createdAt,desc", null, categoryId, tagId, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(card.getId());
    }

    @Test
    void getCards_EnrichesSummaryWithPrimaryMediaType() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setPrimaryMediaId(mediaId);
        cardRepository.save(card);

        PagedCardResponse response =
                cardService.getCards(0, 20, "createdAt,desc", null, null, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPrimaryMediaType())
                .isEqualTo(CardSummaryResponse.PrimaryMediaTypeEnum.PHOTO);
    }

    @Test
    void getCards_EnrichesVideoTypeAndDuration() {
        UUID videoId = UUID.randomUUID();
        insertMediaRow(videoId, MediaType.VIDEO, "video/mp4", 42);
        when(mediaLookup.findPublicSummary(videoId))
                .thenReturn(Optional.of(videoSummary(videoId, 42)));

        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setPrimaryMediaId(videoId);
        cardRepository.save(card);

        PagedCardResponse response =
                cardService.getCards(0, 20, "createdAt,desc", null, null, null, null, null);

        CardSummaryResponse summary = response.getContent().get(0);
        assertThat(summary.getPrimaryMediaType())
                .isEqualTo(CardSummaryResponse.PrimaryMediaTypeEnum.VIDEO);
        assertThat(summary.getDurationSeconds()).isEqualTo(42);
    }


    @Test
    void getMyCards_ReturnsOnlyOwnedCards() {
        UUID otherAuthorId = UUID.randomUUID();
        userRepository.save(User.builder()
                .id(otherAuthorId)
                .username("other-" + otherAuthorId)
                .email("other-" + otherAuthorId + "@example.com")
                .build());

        NatureCard myCard = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        myCard.setAuthorId(authorId);
        NatureCard otherCard = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        otherCard.setAuthorId(otherAuthorId);
        cardRepository.saveAll(List.of(myCard, otherCard));

        PagedCardResponse response =
                cardService.getMyCards(authorId, 0, 20, null, "createdAt,desc");

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(myCard.getId());
    }


    @Test
    void getCardById_EnrichedWithAuthorAndMedia() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setPrimaryMediaId(mediaId);
        cardRepository.save(card);

        CardDetailResponse response = cardService.getCardById(card.getId());

        assertThat(response.getAuthor()).isNotNull();
        assertThat(response.getAuthor().getUsername()).isEqualTo("authoruser");
        assertThat(response.getPrimaryMedia()).isNotNull();
        assertThat(response.getPrimaryMedia().getCdnUrl()).isEqualTo("http://cdn/media.jpg");
    }

    @Test
    void getCardById_EnrichesPrimaryMediaTypeAndDuration() {
        UUID videoId = UUID.randomUUID();
        insertMediaRow(videoId, MediaType.VIDEO, "video/mp4", 90);
        when(mediaLookup.findPublicSummary(videoId))
                .thenReturn(Optional.of(videoSummary(videoId, 90)));

        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setPrimaryMediaId(videoId);
        cardRepository.save(card);

        CardDetailResponse response = cardService.getCardById(card.getId());

        assertThat(response.getPrimaryMediaType())
                .isEqualTo(CardDetailResponse.PrimaryMediaTypeEnum.VIDEO);
        assertThat(response.getDurationSeconds()).isEqualTo(90);
    }

    @Test
    void getCardById_WithAuthenticatedUser_SetsIsFavorited() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        cardRepository.save(card);

        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(authorId, card.getId()))
                .card(card)
                .build());

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", authorId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        CardDetailResponse response = cardService.getCardById(card.getId());
        assertThat(response.getIsFavorited()).isTrue();
    }


    @Test
    void createCard_Success() {
        CreateCardRequest request = new CreateCardRequest()
                .title("Golden Eagle")
                .speciesScientificName("Aquila chrysaetos")
                .primaryMediaId(mediaId)
                .categoryIds(Set.of())
                .tagIds(Set.of());

        CardDetailResponse response = cardService.createCard(request, authorId);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Golden Eagle");
        assertThat(response.getStatus()).isEqualTo(CardDetailResponse.StatusEnum.DRAFT);
    }

    @Test
    void createCard_InvalidMediaReferenceThrows() {
        UUID invalidMediaId = UUID.randomUUID();
        when(mediaLookup.findPublicSummary(invalidMediaId)).thenReturn(Optional.empty());

        CreateCardRequest request = new CreateCardRequest()
                .title("Invalid Media")
                .speciesScientificName("Test")
                .primaryMediaId(invalidMediaId);

        assertThatThrownBy(() -> cardService.createCard(request, authorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Primary media not found");
    }

    @Test
    void updateCard_ByOwnerSucceeds() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.DRAFT, null, null);
        card.setAuthorId(authorId);
        cardRepository.save(card);

        UpdateCardRequest request = new UpdateCardRequest()
                .title("Updated Title")
                .speciesScientificName("Updated species")
                .primaryMediaId(mediaId)
                .status(UpdateCardRequest.StatusEnum.PUBLISHED)
                .categoryIds(Set.of())
                .tagIds(Set.of());

        CardDetailResponse response = cardService.updateCard(card.getId(), request, authorId);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getStatus()).isEqualTo(CardDetailResponse.StatusEnum.PUBLISHED);
    }

    @Test
    void updateCard_ByNonOwnerThrowsForbidden() {
        UUID otherAuthorId = UUID.randomUUID();
        userRepository.save(User.builder()
                .id(otherAuthorId)
                .username("other-" + otherAuthorId)
                .email("other-" + otherAuthorId + "@example.com")
                .build());

        NatureCard card = createCard(UUID.randomUUID(), CardStatus.DRAFT, null, null);
        card.setAuthorId(otherAuthorId);
        cardRepository.save(card);

        when(securityUtils.isOwnerOrAdmin(otherAuthorId)).thenReturn(false);

        UpdateCardRequest request = new UpdateCardRequest()
                .title("Unauthorized")
                .speciesScientificName("Test");

        assertThatThrownBy(() -> cardService.updateCard(card.getId(), request, authorId))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void deleteCard_ByOwnerSucceeds() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setAuthorId(authorId);
        cardRepository.save(card);

        cardService.deleteCard(card.getId(), authorId);

        assertThat(cardRepository.findById(card.getId())).isEmpty();
    }

    @Test
    void deleteCard_RemovesFavoritesFirst() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        cardRepository.save(card);

        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(authorId, card.getId()))
                .card(card)
                .build());

        assertThat(favoriteRepository.findById(new FavoriteId(authorId, card.getId()))).isPresent();

        cardService.deleteCard(card.getId(), authorId);

        assertThat(cardRepository.findById(card.getId())).isEmpty();
        assertThat(favoriteRepository.findById(new FavoriteId(authorId, card.getId()))).isEmpty();
    }

    @Test
    void deleteCard_ByNonOwnerThrowsForbidden() {
        UUID otherAuthorId = UUID.randomUUID();
        userRepository.save(User.builder()
                .id(otherAuthorId)
                .username("other-" + otherAuthorId)
                .email("other-" + otherAuthorId + "@example.com")
                .build());

        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setAuthorId(otherAuthorId);
        cardRepository.save(card);

        when(securityUtils.isOwnerOrAdmin(otherAuthorId)).thenReturn(false);

        assertThatThrownBy(() -> cardService.deleteCard(card.getId(), authorId))
                .isInstanceOf(ForbiddenAccessException.class);
    }


    @Test
    void bulkDelete_AllCardsDeletedSuccessfully() {
        NatureCard a = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        NatureCard b = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        cardRepository.saveAll(List.of(a, b));

        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(authorId, a.getId())).card(a).build());
        favoriteRepository.save(Favorite.builder()
                .id(new FavoriteId(authorId, b.getId())).card(b).build());

        BulkDeleteCardsResponse response =
                cardService.bulkDelete(Set.of(a.getId(), b.getId()), authorId);

        assertThat(response.getDeleted()).isEqualTo(2);
        assertThat(response.getFailed()).isZero();
        assertThat(response.getDeletedIds())
                .containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(cardRepository.findById(a.getId())).isEmpty();
        assertThat(cardRepository.findById(b.getId())).isEmpty();
    }

    @Test
    void bulkDelete_PartialWithMissingId_ReportsPerItemError() {
        NatureCard a = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        cardRepository.save(a);

        UUID missing = UUID.randomUUID();

        BulkDeleteCardsResponse response =
                cardService.bulkDelete(Set.of(a.getId(), missing), authorId);

        assertThat(response.getDeleted()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getDeletedIds()).containsExactly(a.getId());
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).getId()).isEqualTo(missing);
        assertThat(response.getErrors().get(0).getError()).containsIgnoringCase("not found");
    }

    @Test
    void bulkDelete_WithoutPermission_ReportsPerItemError() {
        UUID otherAuthorId = UUID.randomUUID();
        userRepository.save(User.builder()
                .id(otherAuthorId)
                .username("other-" + otherAuthorId)
                .email("other-" + otherAuthorId + "@example.com")
                .build());

        NatureCard owned = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        owned.setAuthorId(authorId);
        NatureCard other = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        other.setAuthorId(otherAuthorId);
        cardRepository.saveAll(List.of(owned, other));

        when(securityUtils.isOwnerOrAdmin(authorId)).thenReturn(true);
        when(securityUtils.isOwnerOrAdmin(otherAuthorId)).thenReturn(false);

        BulkDeleteCardsResponse response =
                cardService.bulkDelete(Set.of(owned.getId(), other.getId()), authorId);

        assertThat(response.getDeleted()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getDeletedIds()).containsExactly(owned.getId());
        assertThat(response.getErrors().get(0).getError()).containsIgnoringCase("permission");
        assertThat(cardRepository.findById(other.getId())).isPresent();
    }

    @Test
    void bulkDelete_EmptySet_ReturnsEmptyResponse() {
        BulkDeleteCardsResponse response = cardService.bulkDelete(Set.of(), authorId);

        assertThat(response.getDeleted()).isZero();
        assertThat(response.getFailed()).isZero();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getDeletedIds()).isEmpty();
    }


    @Test
    void incrementCardView_IncrementsViewCount() {
        NatureCard card = createCard(UUID.randomUUID(), CardStatus.PUBLISHED, null, null);
        card.setViewCount(5L);
        cardRepository.save(card);

        cardService.incrementCardView(card.getId());

        NatureCard updated = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updated.getViewCount()).isEqualTo(6L);
    }


    private void insertMediaRow(UUID id, MediaType type, String mimeType, Integer durationSeconds) {
        MediaAsset asset = MediaAsset.builder()
                .id(id)
                .uploaderId(authorId)
                .type(type)
                .status(MediaStatus.READY)
                .bucketName("test-bucket")
                .fileKey("media/" + id + "/asset")
                .cdnUrl("http://cdn/" + id)
                .thumbnailUrl("http://cdn/" + id + "/thumb")
                .largeUrl("http://cdn/" + id + "/large")
                .fileSizeBytes(1000L)
                .mimeType(mimeType)
                .durationSeconds(durationSeconds)
                .resolutionWidth(1920)
                .resolutionHeight(1080)
                .build();
        mediaAssetRepository.save(asset);
    }

    private NatureCard createCard(UUID id, CardStatus status,
                                  Set<UUID> categoryIds, Set<UUID> tagIds) {
        NatureCard.NatureCardBuilder builder = NatureCard.builder()
                .id(id)
                .authorId(authorId)
                .title("Test Card " + id)
                .speciesScientificName("Test species")
                .status(status)
                .viewCount(0L)
                .favoriteCount(0L);

        builder.categories(categoryIds != null
                ? new HashSet<>(categoryRepository.findAllById(categoryIds))
                : new HashSet<>());

        builder.tags(tagIds != null
                ? new HashSet<>(tagRepository.findAllById(tagIds))
                : new HashSet<>());

        return builder.build();
    }

    private MediaPublicSummary photoSummary(UUID id) {
        return new MediaPublicSummary(
                id,
                "http://cdn/media.jpg",
                "http://cdn/media-thumb.jpg",
                "http://cdn/media-large.jpg",
                "PHOTO",
                null
        );
    }

    private MediaPublicSummary videoSummary(UUID id, int durationSeconds) {
        return new MediaPublicSummary(
                id,
                "http://cdn/clip.mp4",
                "http://cdn/clip-poster.jpg",
                null,
                "VIDEO",
                durationSeconds
        );
    }
}