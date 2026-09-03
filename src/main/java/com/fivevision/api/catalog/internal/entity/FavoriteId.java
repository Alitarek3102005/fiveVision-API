package com.fivevision.api.catalog.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "card_id")
    private UUID cardId;
}