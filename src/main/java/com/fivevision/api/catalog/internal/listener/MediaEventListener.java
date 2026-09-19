package com.fivevision.api.catalog.internal.listener;

import com.fivevision.api.catalog.internal.entity.NatureCard;
import com.fivevision.api.catalog.internal.repository.CardRepository;
import com.fivevision.api.catalog.internal.repository.FavoriteRepository;
import com.fivevision.api.media.internal.event.MediaAssetDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaEventListener {

    private final CardRepository cardRepository;
    private final FavoriteRepository favoriteRepository;

    @ApplicationModuleListener
    public void onMediaDeleted(MediaAssetDeletedEvent event) {
        List<NatureCard> affected = cardRepository
                .findAllByPrimaryMediaIdOrThumbnailMediaId(event.mediaId(), event.mediaId());

        if (affected.isEmpty()) {
            log.info("Media {} deleted; no cards referenced it", event.mediaId());
            return;
        }

        log.warn("Media {} deleted; cascading delete to {} card(s)",
                event.mediaId(), affected.size());

        for (NatureCard card : affected) {
            favoriteRepository.deleteAllByIdCardId(card.getId());
        }

        cardRepository.deleteAll(affected);

        log.info("Deleted {} card(s) that depended on media {}",
                affected.size(), event.mediaId());
    }
}