package com.fivevision.api.identity.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakRoleSyncImpl implements KeycloakRoleSync {

    private static final String TARGET_REALM = "5vision";

    private final Keycloak keycloak;

    @Override
    public void assignRealmRole(UUID userId, String role) {
        RealmResource realmResource = keycloak.realm(TARGET_REALM);
        UserResource userResource = realmResource.users().get(userId.toString());
        RoleRepresentation roleRep = realmResource.roles().get(role).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(roleRep));
    }

    @Override
    public void swapRealmRole(UUID userId, String oldRole, String newRole) {
        RealmResource realmResource = keycloak.realm(TARGET_REALM);
        UserResource userResource = realmResource.users().get(userId.toString());

        if (oldRole != null) {
            try {
                RoleRepresentation oldRoleRep = realmResource.roles().get(oldRole).toRepresentation();
                userResource.roles().realmLevel().remove(Collections.singletonList(oldRoleRep));
            } catch (Exception e) {
                log.warn("Old role {} not found or already removed for user {}", oldRole, userId);
            }
        }

        try {
            RoleRepresentation newRoleRep = realmResource.roles().get(newRole).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(newRoleRep));
        } catch (Exception e) {
            log.error("Failed to assign new role {} in Keycloak for user {}", newRole, userId, e);
            throw new RuntimeException("Failed to update role in Identity Provider");
        }
    }
}