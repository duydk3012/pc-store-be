package com.datn.pc_store.modules.product.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.product.dto.ProductResponse;
import com.datn.pc_store.modules.product.enums.*;
import com.datn.pc_store.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Products (Public)", description = "API xem sản phẩm dành cho khách hàng, không yêu cầu đăng nhập")
public class PublicProductController {
    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Xem danh sách sản phẩm công khai", description = "Lấy sản phẩm ACTIVE và OUT_OF_STOCK chưa bị xóa; hỗ trợ tìm kiếm, lọc và phân trang. Không hiển thị INACTIVE. Mặc định 20 sản phẩm/trang, sắp xếp theo name,asc. Sort hợp lệ: id, name, code, slug, productType, status, brandId, createdDate, createdBy, lastModifiedDate, lastModifiedBy; có thể lặp lại sort, ví dụ sort=name,asc&sort=id,desc.")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @Parameter(description = "Từ khóa tìm kiếm theo tên, mã hoặc slug") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc ACTIVE hoặc OUT_OF_STOCK; INACTIVE luôn trả về danh sách rỗng") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Lọc theo loại sản phẩm") @RequestParam(required = false) ProductType productType,
            @Parameter(description = "Lọc theo ID thương hiệu") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Lọc theo ID danh mục") @RequestParam(required = false) Long categoryId,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getAllProducts(keyword, status, productType, brandId, categoryId, true, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết sản phẩm công khai theo ID", description = "Lấy sản phẩm ACTIVE hoặc OUT_OF_STOCK theo ID. Trả về 404 nếu không tồn tại, INACTIVE hoặc đã bị xóa.")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "ID sản phẩm") @PathVariable Long id
    ) {
        return ResponseEntity.ok(productService.getProductById(id, true));
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "Xem chi tiết sản phẩm công khai theo slug", description = "Lấy sản phẩm ACTIVE hoặc OUT_OF_STOCK theo slug. Trả về 404 nếu không tồn tại, INACTIVE hoặc đã bị xóa.")
    public ResponseEntity<ProductResponse> getProductBySlug(
            @Parameter(description = "Slug sản phẩm") @PathVariable String slug
    ) {
        return ResponseEntity.ok(productService.getProductBySlug(slug, true));
    }
}
