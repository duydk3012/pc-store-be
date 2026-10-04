package com.datn.pc_store.category.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datn.pc_store.category.dto.CategoryRequest;
import com.datn.pc_store.category.entity.Category;
import com.datn.pc_store.category.repository.CategoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void createTrimsFieldsAndMarksCategoryActive() {
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.create(new CategoryRequest("  Hardware  ", "  hardware  "));

        assertEquals("Hardware", response.name());
        assertEquals("hardware", response.slug());
        assertEquals(0, response.isDeleted());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateChangesCategoryFields() {
        Category category = new Category();
        category.setId(42L);
        category.setIsDeleted(0);
        when(categoryRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        var response = categoryService.update(42L, new CategoryRequest("Hardware", "hardware"));

        assertEquals("Hardware", response.name());
        assertEquals("hardware", response.slug());
        verify(categoryRepository).saveAndFlush(category);
    }

    @Test
    void deleteSoftDeletesCategory() {
        Category category = new Category();
        category.setId(42L);
        category.setIsDeleted(0);
        when(categoryRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        categoryService.delete(42L);

        assertEquals(1, category.getIsDeleted());
        verify(categoryRepository).save(category);
    }

    @Test
    void getByIdReturnsNotFoundForMissingOrDeletedCategory() {
        when(categoryRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.getById(42L));

        assertEquals(404, exception.getStatusCode().value());
    }
}
