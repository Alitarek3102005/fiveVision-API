package com.fivevision.api.catalog.internal.listener;

import com.fivevision.api.catalog.internal.entity.NatureCard;
import com.fivevision.api.catalog.internal.repository.CardRepository;
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

    @ApplicationModuleListener
    public void onMediaDeleted(MediaAssetDeletedEvent event) {
        log.info("Received MediaAssetDeletedEvent for mediaId={}", event.mediaId());

        List<NatureCard> affectedCards = cardRepository
                .findAllByPrimaryMediaIdOrThumbnailMediaId(event.mediaId(), event.mediaId());

        for (NatureCard card : affectedCards) {
            boolean changed = false;
            if (event.mediaId().equals(card.getPrimaryMediaId())) {
                card.setPrimaryMediaId(null);
                changed = true;
            }
            if (event.mediaId().equals(card.getThumbnailMediaId())) {
                card.setThumbnailMediaId(null);
                changed = true;
            }
            if (changed) {
                log.debug("Clearing media reference for card id={}", card.getId());
            }
        }
        cardRepository.saveAll(affectedCards);
        log.info("Cleared media references in {} card(s)", affectedCards.size());
    }
}