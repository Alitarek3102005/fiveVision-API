package com.fivevision.api.catalog;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.service.CardService;
import com.fivevision.api.common.security.SecurityUtils;
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
public class CardControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private SecurityUtils securityUtils;


    @Test
    void getCards_Public_ReturnsPagedCards() throws Exception {
        PagedCardResponse response = new PagedCardResponse();
        response.setContent(List.of(new CardSummaryResponse()
                .id(UUID.randomUUID())
                .title("Public Card")));
        response.setPageNumber(0);
        response.setPageSize(20);
        response.setTotalElements(1L);
        response.setTotalPages(1);
        response.setIsLast(true);

        when(cardService.getCards(0, 20, "createdAt,desc", null, null, null, null))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }


    @Test
    void getMyCards_Authenticated_ReturnsOwnedCards() throws Exception {
        UUID userId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        PagedCardResponse response = new PagedCardResponse();
        response.setContent(List.of(new CardSummaryResponse()
                .id(UUID.randomUUID())
                .title("My Card")));
        response.setPageNumber(0);
        response.setPageSize(20);
        response.setTotalElements(1L);
        response.setTotalPages(1);
        response.setIsLast(true);

        when(cardService.getMyCards(eq(userId), eq(0), eq(20), eq(null), eq("updatedAt,desc")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/cards/my-cards")
                        .with(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("My Card"));
    }

    @Test
    void getMyCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cards/my-cards"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void getCardById_Public_ReturnsCard() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardDetailResponse response = new CardDetailResponse()
                .id(cardId)
                .title("Detail Card");

        when(cardService.getCardById(cardId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Detail Card"));
    }


    @Test
    void createCard_WithAuthorRole_ReturnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID primaryMediaId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        CardDetailResponse response = new CardDetailResponse()
                .id(UUID.randomUUID())
                .title("Created Card")
                .status(CardDetailResponse.StatusEnum.DRAFT);

        when(cardService.createCard(any(CreateCardRequest.class), eq(userId)))
                .thenReturn(response);

        String requestBody = """
                {
                  "title": "Created Card",
                  "speciesScientificName": "Test species",
                  "primaryMediaId": "%s"
                }
                """.formatted(primaryMediaId);

        mockMvc.perform(post("/api/v1/cards")
                        .with(jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Created Card"));
    }

    @Test
    void createCard_WithoutRole_Returns403() throws Exception {
        RequestPostProcessor jwt = jwtWithRole("CUSTOMER");
        UUID primaryMediaId = UUID.randomUUID();

        String requestBody = """
                {
                  "title": "Unauthorized",
                  "speciesScientificName": "Test",
                  "primaryMediaId": "%s"
                }
                """.formatted(primaryMediaId);

        mockMvc.perform(post("/api/v1/cards")
                        .with(jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }



    @Test
    void updateCard_Authenticated_ReturnsUpdatedCard() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID primaryMediaId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        CardDetailResponse response = new CardDetailResponse()
                .id(cardId)
                .title("Updated Card")
                .status(CardDetailResponse.StatusEnum.PUBLISHED);

        when(cardService.updateCard(eq(cardId), any(UpdateCardRequest.class), eq(userId)))
                .thenReturn(response);

        String requestBody = """
                {
                  "title": "Updated Card",
                  "speciesScientificName": "Updated species",
                  "primaryMediaId": "%s",
                  "status": "PUBLISHED"
                }
                """.formatted(primaryMediaId);

        mockMvc.perform(put("/api/v1/cards/{id}", cardId)
                        .with(jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Card"));
    }



    @Test
    void deleteCard_Authenticated_ReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId)
                        .with(jwt))
                .andExpect(status().isNoContent());
    }


    @Test
    void incrementCardView_Public_ReturnsNoContent() throws Exception {
        UUID cardId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/cards/{id}/view", cardId))
                .andExpect(status().isNoContent());
    }


    private RequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject("123e4567-e89b-12d3-a456-426614174000")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}