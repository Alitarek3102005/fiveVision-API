package com.fivevision.api.catalog.internal.service;

import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.entity.*;
import com.fivevision.api.catalog.internal.mapper.CardMapper;
import com.fivevision.api.catalog.internal.repository.*;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import com.fivevision.api.common.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CardMapper cardMapper;

    @Transactional(readOnly = true)
    public PagedCardResponse getCards(Integer page, Integer size, String sort, String search, UUID categoryId, UUID tagId, Boolean isPremium) {
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
        return cardMapper.toDetailResponse(card);
    }

    @Transactional
    public CardDetailResponse createCard(CreateCardRequest request, UUID authorId) {
        log.info("User [{}] is creating a new Nature Card: {}", authorId, request.getTitle());

        NatureCard card = NatureCard.builder()
                .authorId(authorId)
                .title(request.getTitle())
                .description(request.getDescription())
                .speciesCommonName(request.getSpeciesCommonName())
                .speciesScientificName(request.getSpeciesScientificName())
                .conservationStatus(request.getConservationStatus())
                .locationName(request.getLocationName())
                .latitude(request.getLatitude() != null ? java.math.BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? java.math.BigDecimal.valueOf(request.getLongitude()) : null)
                .primaryMediaId(request.getPrimaryMediaId())
                .thumbnailMediaId(request.getThumbnailMediaId())
                .isPremium(request.getIsPremium())
                .price(request.getPrice() != null ? java.math.BigDecimal.valueOf(request.getPrice()) : java.math.BigDecimal.ZERO)
                .status(CardStatus.DRAFT)
                .build();

        assignAndValidateTaxonomies(card, request.getCategoryIds(), request.getTagIds());

        NatureCard savedCard = cardRepository.save(card);
        log.info("Successfully created Nature Card [{}]", savedCard.getId());
        return cardMapper.toDetailResponse(savedCard);
    }

    @Transactional
    public CardDetailResponse updateCard(UUID id, UpdateCardRequest request, UUID requesterId) {
        NatureCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + id));

        if (!card.getAuthorId().equals(requesterId)) {
            log.warn("SECURITY ALERT: User [{}] attempted to update Card [{}] owned by [{}]", requesterId, id, card.getAuthorId());
            throw new UnauthorizedAccessException("You do not have permission to modify this card.");
        }

        log.info("User [{}] is updating Nature Card [{}]", requesterId, id);

        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        card.setSpeciesCommonName(request.getSpeciesCommonName());
        card.setSpeciesScientificName(request.getSpeciesScientificName());
        card.setConservationStatus(request.getConservationStatus());
        card.setLocationName(request.getLocationName());

        if (request.getLatitude() != null) card.setLatitude(java.math.BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) card.setLongitude(java.math.BigDecimal.valueOf(request.getLongitude()));

        card.setPrimaryMediaId(request.getPrimaryMediaId());
        card.setThumbnailMediaId(request.getThumbnailMediaId());
        card.setIsPremium(request.getIsPremium());

        if (request.getPrice() != null) card.setPrice(java.math.BigDecimal.valueOf(request.getPrice()));
        if (request.getStatus() != null) card.setStatus(CardStatus.valueOf(request.getStatus().name()));

        assignAndValidateTaxonomies(card, request.getCategoryIds(), request.getTagIds());

        return cardMapper.toDetailResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(UUID id, UUID requesterId) {
        NatureCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + id));

        if (!card.getAuthorId().equals(requesterId)) {
            log.warn("SECURITY ALERT: User [{}] attempted to delete Card [{}] owned by [{}]", requesterId, id, card.getAuthorId());
            throw new UnauthorizedAccessException("You do not have permission to delete this card.");
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
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
    }

    private void assignAndValidateTaxonomies(NatureCard card, List<UUID> categoryIds, List<UUID> tagIds) {
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
}