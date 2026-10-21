package com.datn.pc_store.modules.category.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.category.dto.CategoryResponse;
import com.datn.pc_store.modules.category.dto.CategoryTreeResponse;
import com.datn.pc_store.modules.category.enums.CategoryStatus;
import com.datn.pc_store.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories (Public)", description = "APIs xem danh mục sản phẩm dành cho Khách hàng")
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @Operation(summary = "Lấy cây danh mục sản phẩm đang hoạt động",
               description = "Lấy toàn bộ cây danh mục cấp cha - con đang có trạng thái ACTIVE phục vụ hiển thị menu/navigation bar.")
    public ResponseEntity<List<CategoryTreeResponse>> getActiveCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree(true));
    }

    @GetMapping
    @Operation(summary = "Xem danh sách danh mục đang hoạt động",
               description = "Lấy danh sách các danh mục ACTIVE có hỗ trợ tìm kiếm và phân trang.")
    public ResponseEntity<PageResponse<CategoryResponse>> getActiveCategories(
            @Parameter(description = "Từ khóa tìm kiếm (tên, slug)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo ID danh mục cha")
            @RequestParam(required = false) Long parentId,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(categoryService.getAllCategories(keyword, CategoryStatus.ACTIVE, parentId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết danh mục theo ID",
               description = "Lấy chi tiết danh mục theo ID.")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "ID danh mục")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "Xem chi tiết danh mục theo Slug",
               description = "Lấy chi tiết danh mục theo Slug đường dẫn.")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(
            @Parameter(description = "Slug danh mục")
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }
}
