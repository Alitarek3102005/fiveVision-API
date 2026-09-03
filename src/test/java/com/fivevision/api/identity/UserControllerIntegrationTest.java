package com.fivevision.api.identity;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.identity.internal.dto.*;
import com.fivevision.api.identity.internal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getUsers_Admin_ReturnsPagedUsers() throws Exception {
        RequestPostProcessor adminJwt = jwtWithRole("ADMIN");

        PagedUserResponse response = new PagedUserResponse();
        response.setContent(List.of(new UserProfileResponse()
                .id(UUID.randomUUID())
                .username("adminuser")
                .role(UserProfileResponse.RoleEnum.ADMIN)));
        response.setPageNumber(0);
        response.setPageSize(20);
        response.setTotalElements(1L);
        response.setTotalPages(1);
        response.setIsLast(true);

        when(userService.listUsers(eq(0), eq(20), eq("createdAt,desc"), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/users").with(adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getUsers_NonAdmin_Returns403() throws Exception {
        RequestPostProcessor authorJwt = jwtWithRole("AUTHOR");

        mockMvc.perform(get("/api/v1/users").with(authorJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentUser_Authenticated_ReturnsProfile() throws Exception {
        RequestPostProcessor userJwt = jwtWithRole("CUSTOMER");

        UserProfileResponse response = new UserProfileResponse()
                .id(UUID.randomUUID())
                .username("currentuser")
                .role(UserProfileResponse.RoleEnum.CUSTOMER);

        when(userService.getCurrentUser()).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me").with(userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("currentuser"));
    }

    @Test
    void getCurrentUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentUser_ValidRequest_ReturnsUpdatedProfile() throws Exception {
        RequestPostProcessor userJwt = jwtWithRole("CUSTOMER");

        UserProfileResponse response = new UserProfileResponse()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .username("john.doe");

        when(userService.updateCurrentUser(any(UpdateProfileRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .with(userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void updateCurrentUser_MissingFirstName_Returns400() throws Exception {
        RequestPostProcessor userJwt = jwtWithRole("CUSTOMER");

        String invalidBody = """
                {
                  "lastName": "Doe"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .with(userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void syncUser_Authenticated_ReturnsSyncedProfile() throws Exception {
        RequestPostProcessor userJwt = jwtWithRole("AUTHOR");

        UserProfileResponse response = new UserProfileResponse()
                .id(UUID.randomUUID())
                .username("synceduser")
                .role(UserProfileResponse.RoleEnum.AUTHOR);

        when(userService.syncUser()).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/sync").with(userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("synceduser"));
    }

    @Test
    void getUserById_Public_ReturnsSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        UserSummaryResponse response = new UserSummaryResponse()
                .id(userId)
                .username("publicuser")
                .firstName("Public")
                .lastName("User")
                .role(UserSummaryResponse.RoleEnum.AUTHOR);

        when(userService.getUserSummary(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("publicuser"))
                .andExpect(jsonPath("$.role").value("AUTHOR"));
    }

    private RequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject("123e4567-e89b-12d3-a456-426614174000")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}