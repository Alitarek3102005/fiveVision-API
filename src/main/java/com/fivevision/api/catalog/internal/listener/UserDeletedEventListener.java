package com.fivevision.api.catalog.internal.listener;

import com.fivevision.api.catalog.internal.entity.NatureCard;
import com.fivevision.api.catalog.internal.repository.CardRepository;
import com.fivevision.api.identity.internal.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("catalogUserDeletedEventListener")
@RequiredArgsConstructor
public class UserDeletedEventListener {

    private final CardRepository cardRepository;

    @ApplicationModuleListener
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("Handling UserDeletedEvent for userId={}", event.userId());
        List<NatureCard> cards = cardRepository.findAllByAuthorId(event.userId());
        if (!cards.isEmpty()) {
            cardRepository.deleteAll(cards);
            log.info("Deleted {} cards for user {}", cards.size(), event.userId());
        }
    }
}