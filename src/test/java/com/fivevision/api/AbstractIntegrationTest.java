package com.fivevision.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (SharedPostgresContainer.INSTANCE != null) {
            registry.add("spring.datasource.url", SharedPostgresContainer.INSTANCE::getJdbcUrl);
            registry.add("spring.datasource.username", SharedPostgresContainer.INSTANCE::getUsername);
            registry.add("spring.datasource.password", SharedPostgresContainer.INSTANCE::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/nature_db_test");
            registry.add("spring.datasource.username", () -> "admin");
            registry.add("spring.datasource.password", () -> "password");
        }
    }
}