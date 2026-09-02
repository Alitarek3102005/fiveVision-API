package com.fivevision.api.catalog.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NatureCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Cross-Module References (Stored purely as UUIDs)
    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "primary_media_id")
    private UUID primaryMediaId;

    @Column(name = "thumbnail_media_id")
    private UUID thumbnailMediaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.DRAFT;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "species_common_name")
    private String speciesCommonName;

    @Column(name = "species_scientific_name")
    private String speciesScientificName;

    @Column(name = "conservation_status")
    private String conservationStatus;

    @Column(name = "location_name")
    private String locationName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false;

    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "favorite_count")
    @Builder.Default
    private Long favoriteCount = 0L;

    // Many-to-Many Relationships
    @ManyToMany
    @JoinTable(
            name = "card_categories",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "card_tags",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}