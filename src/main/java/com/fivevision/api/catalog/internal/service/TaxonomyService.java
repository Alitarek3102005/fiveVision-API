package com.fivevision.api.catalog.internal.service;

import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.entity.Category;
import com.fivevision.api.catalog.internal.entity.Tag;
import com.fivevision.api.catalog.internal.mapper.CategoryMapper;
import com.fivevision.api.catalog.internal.mapper.TagMapper;
import com.fivevision.api.catalog.internal.repository.CategoryRepository;
import com.fivevision.api.catalog.internal.repository.TagRepository;
import com.fivevision.api.common.exception.DuplicateResourceException;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxonomyService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;


    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Boolean tree) {
        List<Category> categories;
        if (Boolean.TRUE.equals(tree)) {
            categories = categoryRepository.findByParentIsNull();
        } else {
            categories = categoryRepository.findAll();
        }
        return categories.stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        validateCategoryRequest(request);
        log.info("Creating new Category: {}", request.getName());

        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateResourceException("A category with slug '" + request.getSlug() + "' already exists.");
        }

        Category.CategoryBuilder builder = Category.builder()
                .id(UUID.randomUUID())            // <-- add UUID generation
                .name(request.getName().trim())
                .slug(request.getSlug().trim())
                .description(request.getDescription());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentId()));
            builder.parent(parent);
        }

        Category saved = categoryRepository.save(builder.build());
        log.info("Created category [{}]", saved.getId());
        return categoryMapper.toCategoryResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        validateCategoryRequest(request);
        log.info("Updating Category [{}]", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (!category.getSlug().equals(request.getSlug().trim()) &&
                categoryRepository.findBySlug(request.getSlug().trim()).isPresent()) {
            throw new DuplicateResourceException("A category with slug '" + request.getSlug() + "' already exists.");
        }

        category.setName(request.getName().trim());
        category.setSlug(request.getSlug().trim());
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent.");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        categoryRepository.delete(category);
        log.info("Deleted Category [{}]", id);
    }


    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toTagResponse)
                .toList();
    }

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        validateTagRequest(request);
        log.info("Creating new Tag: {}", request.getName());

        if (tagRepository.findBySlug(request.getSlug().trim()).isPresent()) {
            throw new DuplicateResourceException("A tag with slug '" + request.getSlug() + "' already exists.");
        }

        Tag tag = Tag.builder()
                .id(UUID.randomUUID())           // <-- add UUID generation
                .name(request.getName().trim())
                .slug(request.getSlug().trim())
                .build();

        Tag saved = tagRepository.save(tag);
        log.info("Created tag [{}]", saved.getId());
        return tagMapper.toTagResponse(saved);
    }

    @Transactional
    public TagResponse updateTag(UUID id, CreateTagRequest request) {
        validateTagRequest(request);
        log.info("Updating Tag [{}]", id);

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with ID: " + id));

        if (!tag.getSlug().equals(request.getSlug().trim()) &&
                tagRepository.findBySlug(request.getSlug().trim()).isPresent()) {
            throw new DuplicateResourceException("A tag with slug '" + request.getSlug() + "' already exists.");
        }

        tag.setName(request.getName().trim());
        tag.setSlug(request.getSlug().trim());

        return tagMapper.toTagResponse(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with ID: " + id));
        tagRepository.delete(tag);
        log.info("Deleted Tag [{}]", id);
    }


    private void validateCategoryRequest(CreateCategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (request.getSlug() == null || request.getSlug().isBlank()) {
            throw new IllegalArgumentException("Category slug is required");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("Category name must be ≤ 100 characters");
        }
        if (request.getSlug().length() > 100) {
            throw new IllegalArgumentException("Category slug must be ≤ 100 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new IllegalArgumentException("Category description must be ≤ 500 characters");
        }
    }

    private void validateTagRequest(CreateTagRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        if (request.getSlug() == null || request.getSlug().isBlank()) {
            throw new IllegalArgumentException("Tag slug is required");
        }
        if (request.getName().length() > 50) {
            throw new IllegalArgumentException("Tag name must be ≤ 50 characters");
        }
        if (request.getSlug().length() > 50) {
            throw new IllegalArgumentException("Tag slug must be ≤ 50 characters");
        }
    }
}