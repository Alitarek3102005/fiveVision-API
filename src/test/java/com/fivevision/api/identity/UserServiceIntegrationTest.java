package com.fivevision.api.identity;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.identity.internal.dto.*;
import com.fivevision.api.identity.internal.entity.User;
import com.fivevision.api.identity.internal.repository.UserRepository;
import com.fivevision.api.identity.internal.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Transactional
public class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private SecurityUtils securityUtils;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
    }

    @Test
    void listUsers_ReturnsPaginatedUsers() {
        User user1 = createUser(UUID.randomUUID(), "user1", "user1@example.com", "John", "Doe", "CUSTOMER");
        User user2 = createUser(UUID.randomUUID(), "user2", "user2@example.com", "Jane", "Doe", "AUTHOR");
        userRepository.saveAll(List.of(user1, user2));

        PagedUserResponse response = userService.listUsers(0, 10, "createdAt,desc", null);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(UserProfileResponse::getUsername)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void listUsers_WithSearchFiltersResults() {
        User user1 = createUser(UUID.randomUUID(), "alice", "alice@example.com", "Alice", "Smith", "CUSTOMER");
        User user2 = createUser(UUID.randomUUID(), "bob", "bob@example.com", "Bob", "Johnson", "AUTHOR");
        userRepository.saveAll(List.of(user1, user2));

        PagedUserResponse response = userService.listUsers(0, 10, "createdAt,desc", "alice");

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void listUsers_InvalidPageThrows() {
        assertThatThrownBy(() -> userService.listUsers(-1, 10, "createdAt,desc", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listUsers_InvalidSizeThrows() {
        assertThatThrownBy(() -> userService.listUsers(0, 101, "createdAt,desc", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCurrentUser_ReturnsProfile() {
        User user = createUser(currentUserId, "currentuser", "current@example.com", "Current", "User", "ADMIN");
        userRepository.save(user);

        UserProfileResponse response = userService.getCurrentUser();

        assertThat(response.getId()).isEqualTo(currentUserId);
        assertThat(response.getUsername()).isEqualTo("currentuser");
        assertThat(response.getRole()).isEqualTo(UserProfileResponse.RoleEnum.ADMIN);
    }

    @Test
    void getCurrentUser_UserNotFoundThrows() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCurrentUser_UpdatesProfile() {
        User user = createUser(currentUserId, "updateuser", "update@example.com", "Old", "Name", "CUSTOMER");
        userRepository.save(user);

        UpdateProfileRequest request = new UpdateProfileRequest()
                .firstName("New")
                .lastName("Profile");

        UserProfileResponse response = userService.updateCurrentUser(request);

        assertThat(response.getFirstName()).isEqualTo("New");
        assertThat(response.getLastName()).isEqualTo("Profile");
        assertThat(userRepository.findById(currentUserId).orElseThrow().getFirstName()).isEqualTo("New");
    }

    @Test
    void updateCurrentUser_MissingFirstNameThrows() {
        UpdateProfileRequest request = new UpdateProfileRequest()
                .lastName("Doe");

        assertThatThrownBy(() -> userService.updateCurrentUser(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void syncUser_CreatesNewUserFromJwtClaims() {
        when(securityUtils.getCurrentUsername()).thenReturn("synceduser");
        when(securityUtils.getCurrentEmail()).thenReturn("sync@example.com");
        when(securityUtils.getCurrentFirstName()).thenReturn("Sync");
        when(securityUtils.getCurrentLastName()).thenReturn("User");
        when(securityUtils.getCurrentRoles()).thenReturn(Set.of("AUTHOR"));

        UserProfileResponse response = userService.syncUser();

        assertThat(response.getId()).isEqualTo(currentUserId);
        assertThat(response.getUsername()).isEqualTo("synceduser");
        assertThat(response.getRole()).isEqualTo(UserProfileResponse.RoleEnum.AUTHOR);
        assertThat(userRepository.findById(currentUserId)).isPresent();
    }

    @Test
    void syncUser_UpdatesExistingUser() {
        User existing = createUser(currentUserId, "oldname", "old@example.com", "Old", "Name", "CUSTOMER");
        userRepository.save(existing);

        when(securityUtils.getCurrentUsername()).thenReturn("newname");
        when(securityUtils.getCurrentEmail()).thenReturn("new@example.com");
        when(securityUtils.getCurrentFirstName()).thenReturn("New");
        when(securityUtils.getCurrentLastName()).thenReturn("Name");
        when(securityUtils.getCurrentRoles()).thenReturn(Set.of("ADMIN"));

        userService.syncUser();

        User updated = userRepository.findById(currentUserId).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo("newname");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void getUserSummary_ReturnsPublicSummary() {
        User user = createUser(currentUserId, "summaryuser", "summary@example.com", "Summary", "User", "AUTHOR");
        userRepository.save(user);

        UserSummaryResponse response = userService.getUserSummary(currentUserId);

        assertThat(response.getId()).isEqualTo(currentUserId);
        assertThat(response.getUsername()).isEqualTo("summaryuser");
        assertThat(response.getRole()).isEqualTo(UserSummaryResponse.RoleEnum.AUTHOR);
    }

    @Test
    void getUserSummary_UserNotFoundThrows() {
        assertThatThrownBy(() -> userService.getUserSummary(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User createUser(UUID id, String username, String email, String firstName, String lastName, String role) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .build();
    }
}