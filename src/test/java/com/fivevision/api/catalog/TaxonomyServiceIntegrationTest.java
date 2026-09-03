package com.fivevision.api.catalog;

import com.fivevision.api.AbstractIntegrationTest;
import com.fivevision.api.catalog.internal.dto.*;
import com.fivevision.api.catalog.internal.entity.Category;
import com.fivevision.api.catalog.internal.entity.Tag;
import com.fivevision.api.catalog.internal.repository.CategoryRepository;
import com.fivevision.api.catalog.internal.repository.TagRepository;
import com.fivevision.api.catalog.internal.service.TaxonomyService;
import com.fivevision.api.common.exception.DuplicateResourceException;
import com.fivevision.api.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TaxonomyServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TaxonomyService taxonomyService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }


    @Test
    void getCategories_TreeTrue_ReturnsOnlyTopLevel() {
        Category parent = Category.builder()
                .name("Parent")
                .slug("parent")
                .build();
        categoryRepository.save(parent);

        Category child = Category.builder()
                .name("Child")
                .slug("child")
                .parent(parent)
                .build();
        categoryRepository.save(child);

        List<CategoryResponse> result = taxonomyService.getCategories(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlug()).isEqualTo("parent");
        assertThat(result.get(0).getSubCategories()).hasSize(1);
    }

    @Test
    void getCategories_TreeFalse_ReturnsAll() {
        Category parent = Category.builder()
                .name("Parent")
                .slug("parent")
                .build();
        categoryRepository.save(parent);

        Category child = Category.builder()
                .name("Child")
                .slug("child")
                .parent(parent)
                .build();
        categoryRepository.save(child);

        List<CategoryResponse> result = taxonomyService.getCategories(false);

        assertThat(result).hasSize(2);
    }

    @Test
    void createCategory_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("Birds")
                .slug("birds")
                .description("Bird species");

        CategoryResponse response = taxonomyService.createCategory(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Birds");
        assertThat(categoryRepository.findBySlug("birds")).isPresent();
    }

    @Test
    void createCategory_DuplicateSlugThrows() {
        Category existing = Category.builder()
                .name("Existing")
                .slug("birds")
                .build();
        categoryRepository.save(existing);

        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("Another")
                .slug("birds");

        assertThatThrownBy(() -> taxonomyService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createCategory_BlankNameThrows() {
        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("")
                .slug("valid-slug");

        assertThatThrownBy(() -> taxonomyService.createCategory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void createCategory_WithParent_SetsParent() {
        Category parent = Category.builder()
                .name("Parent")
                .slug("parent")
                .build();
        categoryRepository.save(parent);

        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("Child")
                .slug("child")
                .parentId(parent.getId());

        CategoryResponse response = taxonomyService.createCategory(request);

        assertThat(response.getParentId()).isEqualTo(parent.getId());
    }

    @Test
    void updateCategory_Success() {
        Category existing = Category.builder()
                .name("Old Name")
                .slug("old-slug")
                .build();
        categoryRepository.save(existing);

        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("New Name")
                .slug("new-slug")
                .description("Updated");

        CategoryResponse response = taxonomyService.updateCategory(existing.getId(), request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getSlug()).isEqualTo("new-slug");
    }

    @Test
    void updateCategory_DuplicateSlugThrows() {
        Category existing1 = Category.builder().name("One").slug("one").build();
        Category existing2 = Category.builder().name("Two").slug("two").build();
        categoryRepository.saveAll(List.of(existing1, existing2));

        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("One Updated")
                .slug("two");

        assertThatThrownBy(() -> taxonomyService.updateCategory(existing1.getId(), request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateCategory_SelfParentThrows() {
        Category existing = Category.builder().name("Self").slug("self").build();
        categoryRepository.save(existing);

        CreateCategoryRequest request = new CreateCategoryRequest()
                .name("Self")
                .slug("self")
                .parentId(existing.getId());

        assertThatThrownBy(() -> taxonomyService.updateCategory(existing.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    void deleteCategory_Success() {
        Category existing = Category.builder().name("ToDelete").slug("to-delete").build();
        categoryRepository.save(existing);

        taxonomyService.deleteCategory(existing.getId());

        assertThat(categoryRepository.findById(existing.getId())).isEmpty();
    }

    @Test
    void deleteCategory_NotFoundThrows() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> taxonomyService.deleteCategory(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void getTags_ReturnsAllTags() {
        Tag tag1 = Tag.builder().name("Rare").slug("rare").build();
        Tag tag2 = Tag.builder().name("Common").slug("common").build();
        tagRepository.saveAll(List.of(tag1, tag2));

        List<TagResponse> result = taxonomyService.getTags();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TagResponse::getSlug)
                .containsExactlyInAnyOrder("rare", "common");
    }

    @Test
    void createTag_Success() {
        CreateTagRequest request = new CreateTagRequest()
                .name("Rare")
                .slug("rare");

        TagResponse response = taxonomyService.createTag(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSlug()).isEqualTo("rare");
    }

    @Test
    void createTag_DuplicateSlugThrows() {
        Tag existing = Tag.builder().name("Existing").slug("rare").build();
        tagRepository.save(existing);

        CreateTagRequest request = new CreateTagRequest()
                .name("Another")
                .slug("rare");

        assertThatThrownBy(() -> taxonomyService.createTag(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createTag_BlankNameThrows() {
        CreateTagRequest request = new CreateTagRequest()
                .name("")
                .slug("valid-slug");

        assertThatThrownBy(() -> taxonomyService.createTag(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void updateTag_Success() {
        Tag existing = Tag.builder().name("Old").slug("old").build();
        tagRepository.save(existing);

        CreateTagRequest request = new CreateTagRequest()
                .name("New")
                .slug("new");

        TagResponse response = taxonomyService.updateTag(existing.getId(), request);

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getSlug()).isEqualTo("new");
    }

    @Test
    void updateTag_DuplicateSlugThrows() {
        Tag tag1 = Tag.builder().name("One").slug("one").build();
        Tag tag2 = Tag.builder().name("Two").slug("two").build();
        tagRepository.saveAll(List.of(tag1, tag2));

        CreateTagRequest request = new CreateTagRequest()
                .name("One Updated")
                .slug("two");

        assertThatThrownBy(() -> taxonomyService.updateTag(tag1.getId(), request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteTag_Success() {
        Tag existing = Tag.builder().name("Delete Me").slug("delete-me").build();
        tagRepository.save(existing);

        taxonomyService.deleteTag(existing.getId());

        assertThat(tagRepository.findById(existing.getId())).isEmpty();
    }

    @Test
    void deleteTag_NotFoundThrows() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> taxonomyService.deleteTag(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}