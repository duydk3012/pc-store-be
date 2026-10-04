package com.datn.pc_store.category.service;

import com.datn.pc_store.category.dto.CategoryRequest;
import com.datn.pc_store.category.dto.CategoryResponse;
import com.datn.pc_store.category.entity.Category;
import com.datn.pc_store.category.repository.CategoryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CategoryService {

    private static final int ACTIVE = 0;
    private static final int DELETED = 1;

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByIsDeletedOrderByIdAsc(ACTIVE)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return CategoryResponse.from(findActiveCategory(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        applyRequest(category, request);
        category.setIsDeleted(ACTIVE);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findActiveCategory(id);
        applyRequest(category, request);
        return CategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    public void delete(Long id) {
        Category category = findActiveCategory(id);
        category.setIsDeleted(DELETED);
        categoryRepository.save(category);
    }

    private Category findActiveCategory(Long id) {
        return categoryRepository.findByIdAndIsDeleted(id, ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found: " + id));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim());
    }
}
