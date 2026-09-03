package com.fivevision.api.catalog.internal.service;

import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.entity.*;
import com.fivevision.api.catalog.internal.mapper.CardMapper;
import com.fivevision.api.catalog.internal.repository.*;
import com.fivevision.api.common.exception.ForbiddenAccessException;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.identity.UserLookup;
import com.fivevision.api.identity.UserPublicSummary;
import com.fivevision.api.media.MediaLookup;
import com.fivevision.api.media.MediaPublicSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("createdAt", "updatedAt", "title", "viewCount", "favoriteCount");

    private final CardRepository cardRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CardMapper cardMapper;
    private final SecurityUtils securityUtils;
    private final UserLookup userLookup;
    private final MediaLookup mediaLookup;
    private final FavoriteRepository favoriteRepository;


    @Transactional(readOnly = true)
    public PagedCardResponse getCards(Integer page, Integer size, String sort, String search,
                                      UUID categoryId, UUID tagId, Boolean isPremium) {
        PageRequest pageRequest = buildPageRequest(page, size, sort);

        Specification<NatureCard> spec = Specification.where(CardRepository.hasStatus(CardStatus.PUBLISHED))
                .and(CardRepository.searchKeyword(search))
                .and(CardRepository.hasCategory(categoryId))
                .and(CardRepository.hasTag(tagId))
                .and(CardRepository.isPremium(isPremium));

        Page<NatureCard> cardPage = cardRepository.findAll(spec, pageRequest);
        return cardMapper.toPagedResponse(cardPage);
    }

    @Transactional(readOnly = true)
    public PagedCardResponse getMyCards(UUID authorId, Integer page, Integer size, String status, String sort) {
        PageRequest pageRequest = buildPageRequest(page, size, sort);
        CardStatus cardStatus = status != null ? CardStatus.valueOf(status) : null;

        Specification<NatureCard> spec = Specification.where(CardRepository.hasAuthorId(authorId))
                .and(CardRepository.hasStatus(cardStatus));

        Page<NatureCard> cardPage = cardRepository.findAll(spec, pageRequest);
        return cardMapper.toPagedResponse(cardPage);
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getCardById(UUID id) {
        NatureCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + id));

        CardDetailResponse response = cardMapper.toDetailResponse(card);

        userLookup.findPublicSummary(card.getAuthorId())
                .ifPresent(user -> response.setAuthor(
                        new AuthorSummary()
                                .id(user.id())
                                .username(user.username())
                ));

        if (card.getPrimaryMediaId() != null) {
            mediaLookup.findPublicSummary(card.getPrimaryMediaId())
                    .ifPresent(media -> response.setPrimaryMedia(toMediaSummary(media)));
        }

        if (card.getThumbnailMediaId() != null) {
            mediaLookup.findPublicSummary(card.getThumbnailMediaId())
                    .ifPresent(media -> response.setThumbnailMedia(toMediaSummary(media)));
        }

        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken) {
            UUID currentUserId = securityUtils.getCurrentUserId();
            response.setIsFavorited(
                    favoriteRepository.existsById(new FavoriteId(currentUserId, id))
            );
        }

        return response;
    }


    @Transactional
    public CardDetailResponse createCard(CreateCardRequest request, UUID authorId) {
        log.info("User [{}] is creating a new Nature Card: {}", authorId, request.getTitle());

        NatureCard card = NatureCard.builder()
                .id(UUID.randomUUID())
                .authorId(authorId)
                .title(request.getTitle())
                .description(request.getDescription())
                .speciesCommonName(request.getSpeciesCommonName())
                .speciesScientificName(request.getSpeciesScientificName())
                .conservationStatus(request.getConservationStatus())
                .locationName(request.getLocationName())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .primaryMediaId(request.getPrimaryMediaId())
                .thumbnailMediaId(request.getThumbnailMediaId())
                .isPremium(request.getIsPremium())
                .price(request.getPrice() != null ? BigDecimal.valueOf(request.getPrice()) : BigDecimal.ZERO)
                .status(CardStatus.DRAFT)
                .build();

        assignAndValidateTaxonomies(card, request.getCategoryIds(), request.getTagIds());
        validateMediaReferences(request);
        NatureCard savedCard = cardRepository.save(card);
        log.info("Successfully created Nature Card [{}]", savedCard.getId());
        return cardMapper.toDetailResponse(savedCard);
    }

    @Transactional
    public CardDetailResponse updateCard(UUID id, UpdateCardRequest request, UUID requesterId) {
        NatureCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + id));

        if (!securityUtils.isOwnerOrAdmin(card.getAuthorId())) {
            log.warn("SECURITY ALERT: User [{}] attempted to update Card [{}] owned by [{}]",
                    requesterId, id, card.getAuthorId());
            throw new ForbiddenAccessException("You do not have permission to modify this card.");
        }

        log.info("User [{}] is updating Nature Card [{}]", requesterId, id);

        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        card.setSpeciesCommonName(request.getSpeciesCommonName());
        card.setSpeciesScientificName(request.getSpeciesScientificName());
        card.setConservationStatus(request.getConservationStatus());
        card.setLocationName(request.getLocationName());

        if (request.getLatitude() != null) card.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) card.setLongitude(BigDecimal.valueOf(request.getLongitude()));

        card.setPrimaryMediaId(request.getPrimaryMediaId());
        card.setThumbnailMediaId(request.getThumbnailMediaId());
        card.setIsPremium(request.getIsPremium());

        if (request.getPrice() != null) card.setPrice(BigDecimal.valueOf(request.getPrice()));
        if (request.getStatus() != null) card.setStatus(CardStatus.valueOf(request.getStatus().name()));

        assignAndValidateTaxonomies(card, request.getCategoryIds(), request.getTagIds());
        validateMediaReferences(request);
        return cardMapper.toDetailResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(UUID id, UUID requesterId) {
        NatureCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + id));

        if (!securityUtils.isOwnerOrAdmin(card.getAuthorId())) {
            log.warn("SECURITY ALERT: User [{}] attempted to delete Card [{}] owned by [{}]",
                    requesterId, id, card.getAuthorId());
            throw new ForbiddenAccessException("You do not have permission to delete this card.");
        }

        cardRepository.delete(card);
        log.info("User [{}] successfully deleted Nature Card [{}]", requesterId, id);
    }

    @Transactional
    public void incrementCardView(UUID id) {
        NatureCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + id));
        card.setViewCount(card.getViewCount() + 1);
        cardRepository.save(card);
    }


    private PageRequest buildPageRequest(Integer page, Integer size, String sort) {
        int safePage = (page != null && page >= 0) ? page : 0;
        int safeSize = (size != null && size >= 1 && size <= MAX_PAGE_SIZE) ? size : 20;
        if (size != null && size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be <= " + MAX_PAGE_SIZE);
        }

        Sort sortSpec = parseSort(sort);
        return PageRequest.of(safePage, safeSize, sortSpec);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new IllegalArgumentException("sort property not allowed: " + property);
        }
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private void assignAndValidateTaxonomies(NatureCard card, Set<UUID> categoryIds, Set<UUID> tagIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.size() != categoryIds.size()) {
                throw new IllegalArgumentException("One or more Category IDs provided are invalid or do not exist.");
            }
            card.setCategories(new HashSet<>(categories));
        } else {
            card.setCategories(new HashSet<>());
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(tagIds);
            if (tags.size() != tagIds.size()) {
                throw new IllegalArgumentException("One or more Tag IDs provided are invalid or do not exist.");
            }
            card.setTags(new HashSet<>(tags));
        } else {
            card.setTags(new HashSet<>());
        }
    }

    private MediaSummary toMediaSummary(MediaPublicSummary media) {
        return new MediaSummary()
                .id(media.id())
                .cdnUrl(media.cdnUrl())
                .type(MediaSummary.TypeEnum.fromValue(media.type()));
    }
    private void validateMediaReferences(CreateCardRequest request) {
        if (request.getPrimaryMediaId() != null) {
            mediaLookup.findPublicSummary(request.getPrimaryMediaId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary media not found"));
        }
        if (request.getThumbnailMediaId() != null) {
            mediaLookup.findPublicSummary(request.getThumbnailMediaId())
                    .orElseThrow(() -> new IllegalArgumentException("Thumbnail media not found"));
        }
    }
    private void validateMediaReferences(UpdateCardRequest request) {
        if (request.getPrimaryMediaId() != null) {
            mediaLookup.findPublicSummary(request.getPrimaryMediaId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary media not found"));
        }
        if (request.getThumbnailMediaId() != null) {
            mediaLookup.findPublicSummary(request.getThumbnailMediaId())
                    .orElseThrow(() -> new IllegalArgumentException("Thumbnail media not found"));
        }
    }

}