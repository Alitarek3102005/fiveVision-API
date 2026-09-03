package com.fivevision.api.media.internal.listener;

import com.fivevision.api.media.internal.repository.MediaAssetRepository;
import com.fivevision.api.media.internal.entity.MediaAsset;
import com.fivevision.api.identity.internal.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("mediaUserDeletedEventListener")
@RequiredArgsConstructor
public class UserDeletedEventListener {

    private final MediaAssetRepository mediaAssetRepository;

    @ApplicationModuleListener
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("Handling UserDeletedEvent for userId={}", event.userId());
        List<MediaAsset> assets = mediaAssetRepository.findAllByUploaderId(event.userId());
        if (!assets.isEmpty()) {
            mediaAssetRepository.deleteAll(assets);
            log.info("Deleted {} media assets for user {}", assets.size(), event.userId());
        }
    }
}