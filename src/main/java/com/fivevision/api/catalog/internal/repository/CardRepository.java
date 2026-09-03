package com.fivevision.api.catalog.internal.repository;

import com.fivevision.api.catalog.internal.entity.CardStatus;
import com.fivevision.api.catalog.internal.entity.NatureCard;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<NatureCard, UUID>, JpaSpecificationExecutor<NatureCard> {

    static Specification<NatureCard> hasStatus(CardStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
    List<NatureCard> findAllByPrimaryMediaIdOrThumbnailMediaId(UUID primaryMediaId, UUID thumbnailMediaId);
    List<NatureCard> findAllByAuthorId(UUID authorId);

    static Specification<NatureCard> hasAuthorId(UUID authorId) {
        return (root, query, cb) -> authorId == null ? null : cb.equal(root.get("authorId"), authorId);
    }

    static Specification<NatureCard> isPremium(Boolean isPremium) {
        return (root, query, cb) -> isPremium == null ? null : cb.equal(root.get("isPremium"), isPremium);
    }

    static Specification<NatureCard> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("speciesCommonName")), pattern),
                    cb.like(cb.lower(root.get("speciesScientificName")), pattern),
                    cb.like(cb.lower(root.get("locationName")), pattern)
            );
        };
    }

    static Specification<NatureCard> hasCategory(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.equal(root.join("categories").get("id"), categoryId);
        };
    }

    static Specification<NatureCard> hasTag(UUID tagId) {
        return (root, query, cb) -> {
            if (tagId == null) return null;
            return cb.equal(root.join("tags").get("id"), tagId);
        };
    }
}