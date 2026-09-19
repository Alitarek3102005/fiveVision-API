package com.fivevision.api;

import org.testcontainers.containers.PostgreSQLContainer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SharedPostgresContainer {

    public static final PostgreSQLContainer<?> INSTANCE = createContainer();

    private static PostgreSQLContainer<?> createContainer() {
        try {
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nature_db_test")
                    .withUsername("admin")
                    .withPassword("password");
            container.start();
            return container;
        } catch (Exception e) {
            log.warn("Docker not available, falling back to local PostgreSQL.");
            return null;
        }
    }

    private SharedPostgresContainer() {}
}