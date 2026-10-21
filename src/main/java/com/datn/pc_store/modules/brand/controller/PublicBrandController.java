package com.datn.pc_store.modules.brand.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.brand.dto.BrandResponse;
import com.datn.pc_store.modules.brand.enums.BrandStatus;
import com.datn.pc_store.modules.brand.service.BrandService;
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
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Tag(name = "Brands (Public)", description = "APIs xem thương hiệu sản phẩm dành cho Khách hàng")
public class PublicBrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "Xem danh sách thương hiệu đang hoạt động",
               description = "Lấy danh sách các thương hiệu có trạng thái ACTIVE hỗ trợ tìm kiếm và phân trang.")
    public ResponseEntity<PageResponse<BrandResponse>> getActiveBrands(
            @Parameter(description = "Từ khóa tìm kiếm (tên, slug)")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(brandService.getAllBrands(keyword, BrandStatus.ACTIVE, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tất cả thương hiệu đang hoạt động không phân trang",
               description = "Lấy toàn bộ danh sách thương hiệu ACTIVE để hiển thị dropdown hoặc filter.")
    public ResponseEntity<List<BrandResponse>> getAllActiveBrandsWithoutPaging() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết thương hiệu theo ID",
               description = "Lấy chi tiết thương hiệu theo ID.")
    public ResponseEntity<BrandResponse> getBrandById(
            @Parameter(description = "ID thương hiệu")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "Xem chi tiết thương hiệu theo Slug",
               description = "Lấy chi tiết thương hiệu theo Slug đường dẫn.")
    public ResponseEntity<BrandResponse> getBrandBySlug(
            @Parameter(description = "Slug thương hiệu")
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(brandService.getBrandBySlug(slug));
    }
}
