package com.datn.pc_store.category.service;

import com.datn.pc_store.category.dto.CategoryRequest;
import com.datn.pc_store.category.dto.CategoryResponse;
import com.datn.pc_store.category.entity.Category;
import com.datn.pc_store.category.mapper.CategoryMapper;
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
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByIsDeletedOrderByIdAsc(ACTIVE)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findActiveCategory(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        category.setIsDeleted(ACTIVE);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findActiveCategory(id);
        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(categoryRepository.saveAndFlush(category));
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

}
