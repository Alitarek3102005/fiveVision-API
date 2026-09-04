package com.fivevision.api;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {

    public static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nature_db_test")
                    .withUsername("admin")
                    .withPassword("password");

    static {
        INSTANCE.start();
    }

    private SharedPostgresContainer() {}
}