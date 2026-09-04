package com.fivevision.api.media.internal.service;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClamAvHealthIndicator implements HealthIndicator {

    private final ClamAvScanService clamAvScanService;

    @Override
    public Health health() {
        boolean reachable = clamAvScanService.ping();
        if (reachable) {
            return Health.up().build();
        } else {
            return Health.down().withDetail("ClamAV", "Unreachable").build();
        }
    }
}