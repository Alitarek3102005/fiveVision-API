package com.fivevision.api.catalog.internal.controller;

import com.fivevision.api.catalog.internal.api.TaxonomyApi;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.service.TaxonomyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaxonomyController implements TaxonomyApi {

    private final TaxonomyService taxonomyService;

    @Override
    public ResponseEntity<CategoryResponse> createCategory(CreateCategoryRequest createCategoryRequest) {
        CategoryResponse createdCategory = taxonomyService.createCategory(createCategoryRequest);
        return ResponseEntity
                .created(URI.create("/api/v1/taxonomy/categories/" + createdCategory.getId()))
                .body(createdCategory);
    }

    @Override
    public ResponseEntity<TagResponse> createTag(CreateTagRequest createTagRequest) {
        TagResponse createdTag = taxonomyService.createTag(createTagRequest);
        return ResponseEntity
                .created(URI.create("/api/v1/taxonomy/tags/" + createdTag.getId()))
                .body(createdTag);
    }

    @Override
    public ResponseEntity<Void> deleteCategory(UUID id) {
        taxonomyService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTag(UUID id) {
        taxonomyService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CategoryResponse>> getCategories(Boolean tree) {
        return ResponseEntity.ok(taxonomyService.getCategories(tree));
    }

    @Override
    public ResponseEntity<List<TagResponse>> getTags() {
        return ResponseEntity.ok(taxonomyService.getTags());
    }

    @Override
    public ResponseEntity<CategoryResponse> updateCategory(UUID id, CreateCategoryRequest createCategoryRequest) {
        return ResponseEntity.ok(taxonomyService.updateCategory(id, createCategoryRequest));
    }

    @Override
    public ResponseEntity<TagResponse> updateTag(UUID id, CreateTagRequest createTagRequest) {
        return ResponseEntity.ok(taxonomyService.updateTag(id, createTagRequest));
    }
}