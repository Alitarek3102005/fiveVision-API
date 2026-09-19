package com.fivevision.api.identity.internal.service;

import com.fivevision.api.common.exception.ResourceNotFoundException;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.identity.internal.dto.AdminUpdateUserRequest;
import com.fivevision.api.identity.internal.dto.PagedUserResponse;
import com.fivevision.api.identity.internal.dto.UpdateProfileRequest;
import com.fivevision.api.identity.internal.dto.UserProfileResponse;
import com.fivevision.api.identity.internal.dto.UserSummaryResponse;
import com.fivevision.api.identity.internal.entity.User;
import com.fivevision.api.identity.internal.mapper.UserMapper;
import com.fivevision.api.identity.internal.repository.UserRepository;
import com.fivevision.api.identity.internal.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("createdAt", "username", "email", "lastLogin");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final KeycloakRoleSync keycloakRoleSync;

    @Override
    @Transactional(readOnly = true)
    public PagedUserResponse listUsers(int page, int size, String sort, String search) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Page<User> result = userRepository.findAll(
                UserSpecifications.search(search),
                PageRequest.of(page, size, parseSort(sort))
        );

        List<UserProfileResponse> content = result.getContent().stream()
                .map(userMapper::toProfileResponse)
                .toList();

        PagedUserResponse response = new PagedUserResponse();
        response.setContent(content);
        response.setPageNumber(result.getNumber());
        response.setPageSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setIsLast(result.isLast());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUser(UpdateProfileRequest request) {
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("firstName is required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new IllegalArgumentException("lastName is required");
        }

        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user = userRepository.save(user);

        log.info("User {} updated profile", userId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse syncUser() {
        UUID userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUsername();
        String email = securityUtils.getCurrentEmail();
        String firstName = securityUtils.getCurrentFirstName();
        String lastName = securityUtils.getCurrentLastName();
        String role = derivePrimaryRole(securityUtils.getCurrentRoles());

        boolean isNewUser = !userRepository.existsById(userId);

        User user = userRepository.findById(userId)
                .orElse(User.builder().id(userId).build());

        user.setUsername(username);
        if (email != null) user.setEmail(email);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        user.setLastLogin(OffsetDateTime.now());
        user.setRole(role);

        if (isNewUser) {
            try {
                keycloakRoleSync.assignRealmRole(userId, role);
                log.info("New user {} synced and assigned {} role explicitly in Keycloak", userId, role);
            } catch (Exception e) {
                log.warn("New user {} created locally but failed to sync {} role to Keycloak: {}",
                        userId, role, e.getMessage());
            }
        }

        user = userRepository.save(user);
        log.info("User {} synchronized from Keycloak", userId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummary(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toSummaryResponse(user);
    }


    @Override
    @Transactional
    public UserProfileResponse adminUpdateUser(UUID id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getRole() != null) {
            String newRole = request.getRole().name();
            if (!newRole.equals(user.getRole())) {
                keycloakRoleSync.swapRealmRole(user.getId(), user.getRole(), newRole);
                log.info("Admin swapped role for user [{}] from {} to {}", id, user.getRole(), newRole);
                user.setRole(newRole);
            }
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        user = userRepository.save(user);
        log.info("Admin updated profile and permissions for user [{}]", id);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        userRepository.delete(user);
        log.info("Admin permanently deleted user [{}]", id);
    }


    private String derivePrimaryRole(Set<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("AUTHOR")) return "AUTHOR";
        return "CUSTOMER";
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new IllegalArgumentException("sort property not allowed: " + property);
        }
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}