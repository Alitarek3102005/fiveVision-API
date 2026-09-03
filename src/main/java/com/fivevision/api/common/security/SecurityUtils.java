package com.fivevision.api.common.security;

import com.fivevision.api.common.exception.UnauthorizedAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SecurityUtils {

    private static final String ADMIN_ROLE = "ADMIN";

    public UUID getCurrentUserId() {
        Jwt jwt = currentJwt();
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedAccessException("Invalid user identity token format");
        }
    }

    public Set<String> getCurrentRoles() {
        Jwt jwt = currentJwt();
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            Set<String> result = new HashSet<>();
            for (Object role : roles) {
                result.add(String.valueOf(role).toUpperCase());
            }
            return result;
        }
        return Collections.emptySet();
    }

    public boolean hasRole(String role) {
        return getCurrentRoles().contains(role.toUpperCase());
    }

    public boolean isAdmin() {
        return hasRole(ADMIN_ROLE);
    }

    public boolean isOwnerOrAdmin(UUID resourceOwnerId) {
        return isAdmin() || getCurrentUserId().equals(resourceOwnerId);
    }

    public String getCurrentUsername() {
        Jwt jwt = currentJwt();
        Object username = jwt.getClaims().get("preferred_username");
        return username != null ? username.toString() : jwt.getSubject();
    }

    public String getCurrentEmail() {
        Jwt jwt = currentJwt();
        Object email = jwt.getClaims().get("email");
        return email != null ? email.toString() : null;
    }

    public String getCurrentFirstName() {
        Jwt jwt = currentJwt();
        Object givenName = jwt.getClaims().get("given_name");
        return givenName != null ? givenName.toString() : null;
    }

    public String getCurrentLastName() {
        Jwt jwt = currentJwt();
        Object familyName = jwt.getClaims().get("family_name");
        return familyName != null ? familyName.toString() : null;
    }

    private Jwt currentJwt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new UnauthorizedAccessException("No authenticated JWT principal found");
    }
}