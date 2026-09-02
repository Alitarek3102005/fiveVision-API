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

    // --- CATEGORY OPERATIONS ---

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Boolean tree) {
        List<Category> categories;
        if (Boolean.TRUE.equals(tree)) {
            // Only fetch top-level categories; children are mapped automatically via entity relations
            categories = categoryRepository.findByParentIsNull();
        } else {
            categories = categoryRepository.findAll();
        }
        return categories.stream().map(categoryMapper::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating new Category: {}", request.getName());

        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateResourceException("A category with slug '" + request.getSlug() + "' already exists.");
        }

        Category.CategoryBuilder categoryBuilder = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentId()));
            categoryBuilder.parent(parent);
        }

        Category savedCategory = categoryRepository.save(categoryBuilder.build());
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        log.info("Updating Category [{}]", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (!category.getSlug().equals(request.getSlug()) && categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateResourceException("A category with slug '" + request.getSlug() + "' already exists.");
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
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
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Deleted Category [{}]", id);
    }

    // --- TAG OPERATIONS ---

    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        return tagRepository.findAll().stream().map(tagMapper::toTagResponse).toList();
    }

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        log.info("Creating new Tag: {}", request.getName());

        if (tagRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateResourceException("A tag with slug '" + request.getSlug() + "' already exists.");
        }

        Tag tag = Tag.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .build();

        Tag savedTag = tagRepository.save(tag);
        return tagMapper.toTagResponse(savedTag);
    }

    @Transactional
    public TagResponse updateTag(UUID id, CreateTagRequest request) {
        log.info("Updating Tag [{}]", id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with ID: " + id));

        if (!tag.getSlug().equals(request.getSlug()) && tagRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateResourceException("A tag with slug '" + request.getSlug() + "' already exists.");
        }

        tag.setName(request.getName());
        tag.setSlug(request.getSlug());

        return tagMapper.toTagResponse(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found with ID: " + id);
        }
        tagRepository.deleteById(id);
        log.info("Deleted Tag [{}]", id);
    }
}