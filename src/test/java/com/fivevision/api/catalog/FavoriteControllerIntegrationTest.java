package com.fivevision.api.catalog;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.catalog.internal.dto.CardSummaryResponse;
import com.fivevision.api.catalog.internal.dto.PagedCardResponse;
import com.fivevision.api.catalog.internal.service.FavoriteService;
import com.fivevision.api.common.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class FavoriteControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private SecurityUtils securityUtils;


    @Test
    void getFavoriteCards_Authenticated_ReturnsPagedCards() throws Exception {
        UUID userId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        PagedCardResponse response = new PagedCardResponse();
        response.setContent(List.of(new CardSummaryResponse()
                .id(UUID.randomUUID())
                .title("Favorited Card")));
        response.setPageNumber(0);
        response.setPageSize(20);
        response.setTotalElements(1L);
        response.setTotalPages(1);
        response.setIsLast(true);

        when(favoriteService.getFavoriteCards(eq(userId), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/cards/favorites")
                        .with(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getFavoriteCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cards/favorites"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void favoriteCard_Authenticated_ReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(post("/api/v1/cards/{id}/favorite", cardId)
                        .with(jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void favoriteCard_Unauthenticated_Returns401() throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/cards/{id}/favorite", cardId))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void unfavoriteCard_Authenticated_ReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(delete("/api/v1/cards/{id}/favorite", cardId)
                        .with(jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void unfavoriteCard_Unauthenticated_Returns401() throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/cards/{id}/favorite", cardId))
                .andExpect(status().isUnauthorized());
    }


    private RequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject("123e4567-e89b-12d3-a456-426614174000")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of(role))));
    }
}