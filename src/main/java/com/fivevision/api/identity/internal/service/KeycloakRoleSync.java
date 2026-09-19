package com.fivevision.api.identity.internal.service;

import java.util.UUID;

public interface KeycloakRoleSync {

    void assignRealmRole(UUID userId, String role);

    void swapRealmRole(UUID userId, String oldRole, String newRole);
}