package com.fivevision.api.media;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.common.security.SecurityUtils;
import com.fivevision.api.media.internal.dto.CompleteUploadRequest;
import com.fivevision.api.media.internal.dto.InitiateUploadRequest;
import com.fivevision.api.media.internal.dto.InitiateUploadResponse;
import com.fivevision.api.media.internal.dto.MediaAssetResponse;
import com.fivevision.api.media.internal.dto.PagedMediaResponse;
import com.fivevision.api.media.internal.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class MediaControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Test
    void getMediaAssets_ShouldReturnPagedList() throws Exception {
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());

        PagedMediaResponse response = new PagedMediaResponse();
        response.setContent(List.of(new MediaAssetResponse()
                .id(UUID.randomUUID())
                .type(MediaAssetResponse.TypeEnum.PHOTO)
                .status(MediaAssetResponse.StatusEnum.READY)));
        response.setPageNumber(0);
        response.setPageSize(20);
        response.setTotalElements(1L);
        response.setTotalPages(1);
        response.setIsLast(true);

        when(mediaService.listMedia(eq(0), eq(20), eq("createdAt,desc"), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/media").with(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMediaAssets_WithoutRoleShouldReturn403() throws Exception {
        RequestPostProcessor jwt = jwtWithRole("CUSTOMER");

        mockMvc.perform(get("/api/v1/media").with(jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void initiateUpload_ShouldReturnUploadUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        InitiateUploadResponse response = new InitiateUploadResponse()
                .mediaId(UUID.randomUUID())
                .uploadUrl(URI.create("http://minio/upload"))
                .fileKey("media/test.jpg")
                .expiresAt(OffsetDateTime.now().plusMinutes(15));

        when(mediaService.initiateUpload(any(InitiateUploadRequest.class), eq(userId)))
                .thenReturn(response);

        String requestBody = """
                {
                  "fileName": "test.jpg",
                  "mimeType": "image/jpeg",
                  "sizeBytes": 1000,
                  "type": "PHOTO"
                }
                """;

        mockMvc.perform(post("/api/v1/media/upload-url")
                        .with(jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("http://minio/upload"));
    }

    @Test
    void completeUpload_ShouldReturnReadyAsset() throws Exception {
        UUID mediaId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");

        MediaAssetResponse response = new MediaAssetResponse()
                .id(mediaId)
                .status(MediaAssetResponse.StatusEnum.READY);

        when(mediaService.completeUpload(eq(mediaId), any(CompleteUploadRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/media/{id}/complete", mediaId)
                        .with(jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void getMediaById_ShouldReturnAsset() throws Exception {
        UUID mediaId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");

        MediaAssetResponse response = new MediaAssetResponse()
                .id(mediaId)
                .type(MediaAssetResponse.TypeEnum.PHOTO);

        when(mediaService.getById(mediaId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/media/{id}", mediaId).with(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mediaId.toString()));
    }

    @Test
    void deleteMedia_ShouldReturnNoContent() throws Exception {
        UUID mediaId = UUID.randomUUID();
        RequestPostProcessor jwt = jwtWithRole("AUTHOR");

        mockMvc.perform(delete("/api/v1/media/{id}", mediaId).with(jwt))
                .andExpect(status().isNoContent());
    }

    private RequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(token -> token.subject("123e4567-e89b-12d3-a456-426614174000")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}