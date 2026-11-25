package com.datn.pc_store.modules.category.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.common.util.SlugUtils;
import com.datn.pc_store.modules.category.dto.CategoryCreateRequest;
import com.datn.pc_store.modules.category.dto.CategoryResponse;
import com.datn.pc_store.modules.category.dto.CategoryTreeResponse;
import com.datn.pc_store.modules.category.dto.CategoryUpdateRequest;
import com.datn.pc_store.modules.category.entity.Category;
import com.datn.pc_store.modules.category.enums.CategoryStatus;
import com.datn.pc_store.modules.category.mapper.CategoryMapper;
import com.datn.pc_store.modules.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "slug", "status", "createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy"
    );

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategories(
            String keyword,
            CategoryStatus status,
            Long parentId,
            Pageable pageable
    ) {
        for (var order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + order.getProperty());
            }
        }
        log.info("Lấy danh sách danh mục với keyword: '{}', status: '{}', parentId: {}, pageable: {}",
                keyword, status, parentId, pageable);
        Page<Category> page = categoryRepository.searchCategories(keyword, status, parentId, pageable);
        return PageResponse.of(page.map(categoryMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree(boolean onlyActive) {
        log.info("Lấy cây danh mục đa cấp, onlyActive: {}", onlyActive);
        List<Category> allCategories = onlyActive
                ? categoryRepository.findByStatus(CategoryStatus.ACTIVE)
                : categoryRepository.findAll();

        Map<Long, CategoryTreeResponse> nodeMap = new HashMap<>();
        List<CategoryTreeResponse> roots = new ArrayList<>();

        for (Category c : allCategories) {
            CategoryTreeResponse node = categoryMapper.toTreeResponse(c);
            node.setChildren(new ArrayList<>());
            nodeMap.put(c.getId(), node);
        }

        for (Category c : allCategories) {
            CategoryTreeResponse node = nodeMap.get(c.getId());
            Long parentId = c.getParent() != null ? c.getParent().getId() : null;
            if (parentId == null) {
                roots.add(node);
            } else {
                CategoryTreeResponse parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return getCategoryById(id, false);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id, boolean publicOnly) {
        log.info("Lấy thông tin danh mục theo id: {}, publicOnly: {}", id, publicOnly);
        Category category = categoryRepository.findById(id)
                .filter(c -> !publicOnly || c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));
        return categoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        return getCategoryBySlug(slug, false);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug, boolean publicOnly) {
        log.info("Lấy thông tin danh mục theo slug: {}, publicOnly: {}", slug, publicOnly);
        Category category = categoryRepository.findBySlug(slug)
                .filter(c -> !publicOnly || c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "slug", slug));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest req) {
        log.info("Thêm mới danh mục: {}", req.getName());

        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? SlugUtils.toSlug(req.getSlug())
                : SlugUtils.toSlug(req.getName());

        if (slug.isBlank()) {
            throw new BadRequestException("Tên danh mục không hợp lệ để tạo slug đường dẫn.");
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Danh mục", "slug", slug);
        }

        Category category = categoryMapper.toEntity(req);
        category.setSlug(slug);

        if (req.getStatus() == null) {
            category.setStatus(CategoryStatus.ACTIVE);
        }

        if (req.getParentId() != null) {
            Category parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục cha", "id", req.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Thêm mới danh mục thành công với id: {}, slug: {}", saved.getId(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest req) {
        log.info("Cập nhật danh mục id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? SlugUtils.toSlug(req.getSlug())
                : SlugUtils.toSlug(req.getName());

        if (slug.isBlank()) {
            throw new BadRequestException("Tên danh mục không hợp lệ để tạo slug đường dẫn.");
        }

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new DuplicateResourceException("Danh mục", "slug", slug);
        }

        categoryMapper.updateEntityFromDto(req, category);
        category.setSlug(slug);

        if (req.getStatus() != null) {
            category.setStatus(req.getStatus());
        }

        // Xử lý quan hệ phân cấp cha - con
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new BadRequestException("Danh mục không thể chọn chính nó làm danh mục cha.");
            }

            Category parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục cha", "id", req.getParentId()));

            if (isDescendant(parent, id)) {
                throw new BadRequestException("Không thể chọn danh mục con làm danh mục cha (gây vòng lặp phân cấp).");
            }

            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category updated = categoryRepository.save(category);
        log.info("Cập nhật danh mục id: {} thành công", updated.getId());
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public CategoryResponse updateStatus(Long id, CategoryStatus status) {
        log.info("Cập nhật trạng thái danh mục id: {} sang: {}", id, status);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        category.setStatus(status);
        Category updated = categoryRepository.save(category);
        log.info("Cập nhật trạng thái danh mục id: {} thành công", updated.getId());
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Yêu cầu xóa mềm danh mục id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        // Kiểm tra xem danh mục có danh mục con còn hoạt động không
        if (categoryRepository.existsByParentId(id)) {
            throw new BadRequestException("Không thể xóa danh mục đang có danh mục con. Vui lòng di chuyển hoặc xóa các danh mục con trước.");
        }

        // Soft delete bằng cách gọi delete (được kích hoạt thông qua @SQLDelete trên Entity)
        categoryRepository.delete(category);
        log.info("Đã xóa mềm danh mục id: {} thành công", id);
    }

    private boolean isDescendant(Category potentialParent, Long targetId) {
        Category current = potentialParent;
        while (current != null) {
            if (current.getId().equals(targetId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
