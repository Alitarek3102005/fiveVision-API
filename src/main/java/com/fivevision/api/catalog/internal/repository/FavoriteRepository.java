package com.fivevision.api.catalog.internal.repository;

import com.fivevision.api.catalog.internal.entity.Favorite;
import com.fivevision.api.catalog.internal.entity.FavoriteId;
import com.fivevision.api.catalog.internal.entity.NatureCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    boolean existsByIdUserIdAndIdCardId(UUID userId, UUID cardId);

    @Query("SELECT f.card FROM Favorite f WHERE f.id.userId = :userId")
    Page<NatureCard> findFavoritedCardsByUserId(@Param("userId") UUID userId, Pageable pageable);
}