package com.fivevision.api.catalog;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.service.TaxonomyService;
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
public class TaxonomyControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxonomyService taxonomyService;


    @Test
    void getCategories_Public_ReturnsList() throws Exception {
        CategoryResponse category = new CategoryResponse()
                .id(UUID.randomUUID())
                .name("Birds")
                .slug("birds");

        when(taxonomyService.getCategories(any(Boolean.class)))
                .thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Birds"));
    }

    @Test
    void getTags_Public_ReturnsList() throws Exception {
        TagResponse tag = new TagResponse()
                .id(UUID.randomUUID())
                .name("Rare")
                .slug("rare");

        when(taxonomyService.getTags()).thenReturn(List.of(tag));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Rare"));
    }


    @Test
    void createCategory_Admin_ReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryResponse created = new CategoryResponse()
                .id(id)
                .name("Birds")
                .slug("birds");

        when(taxonomyService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(created);

        String body = """
                {
                  "name": "Birds",
                  "slug": "birds"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Birds"));
    }

    @Test
    void createCategory_NonAdmin_Returns403() throws Exception {
        String body = """
                {
                  "name": "Birds",
                  "slug": "birds"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .with(authorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_InvalidBody_Returns400() throws Exception {
        String body = """
                {
                  "slug": "birds"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTag_Admin_ReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        TagResponse created = new TagResponse()
                .id(id)
                .name("Rare")
                .slug("rare");

        when(taxonomyService.createTag(any(CreateTagRequest.class)))
                .thenReturn(created);

        String body = """
                {
                  "name": "Rare",
                  "slug": "rare"
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Rare"));
    }

    @Test
    void createTag_NonAdmin_Returns403() throws Exception {
        String body = """
                {
                  "name": "Rare",
                  "slug": "rare"
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .with(authorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCategory_Admin_ReturnsUpdatedCategory() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryResponse updated = new CategoryResponse()
                .id(id)
                .name("Updated Birds")
                .slug("birds");

        when(taxonomyService.updateCategory(eq(id), any(CreateCategoryRequest.class)))
                .thenReturn(updated);

        String body = """
                {
                  "name": "Updated Birds",
                  "slug": "birds"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Birds"));
    }

    @Test
    void updateCategory_NonAdmin_Returns403() throws Exception {
        UUID id = UUID.randomUUID();
        String body = """
                {
                  "name": "Updated Birds",
                  "slug": "birds"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .with(authorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTag_Admin_ReturnsUpdatedTag() throws Exception {
        UUID id = UUID.randomUUID();
        TagResponse updated = new TagResponse()
                .id(id)
                .name("Very Rare")
                .slug("rare");

        when(taxonomyService.updateTag(eq(id), any(CreateTagRequest.class)))
                .thenReturn(updated);

        String body = """
                {
                  "name": "Very Rare",
                  "slug": "rare"
                }
                """;

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Very Rare"));
    }

    @Test
    void updateTag_NonAdmin_Returns403() throws Exception {
        UUID id = UUID.randomUUID();
        String body = """
                {
                  "name": "Very Rare",
                  "slug": "rare"
                }
                """;

        mockMvc.perform(put("/api/v1/tags/{id}", id)
                        .with(authorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategory_Admin_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/categories/{id}", id)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_NonAdmin_Returns403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/categories/{id}", id)
                        .with(authorJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTag_Admin_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/tags/{id}", id)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTag_NonAdmin_Returns403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/tags/{id}", id)
                        .with(authorJwt()))
                .andExpect(status().isForbidden());
    }


    private RequestPostProcessor adminJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject("admin-user-id")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ADMIN"))))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor authorJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject("author-user-id")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of("AUTHOR"))))
                .authorities(new SimpleGrantedAuthority("ROLE_AUTHOR"));
    }
}