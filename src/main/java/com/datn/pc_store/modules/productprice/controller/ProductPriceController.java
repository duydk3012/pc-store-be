package com.datn.pc_store.modules.productprice.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.productprice.dto.*;
import com.datn.pc_store.modules.productprice.service.ProductPriceService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product-prices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Product Prices (Admin)", description = "Tra cứu lịch sử giá sản phẩm dành cho Admin")
public class ProductPriceController {
    private final ProductPriceService productPriceService;

    @GetMapping
    @Operation(summary = "Xem danh sách và lịch sử giá", description = "Phân trang, lọc theo sản phẩm và cờ isPresentPrice. Cờ được chọn áp dụng còn phải thỏa khoảng hiệu lực để là giá hiện tại. Mặc định sort=createdDate,desc. Trường sort hợp lệ: id, productId, price, effectiveFrom, effectiveTo, presentPrice, createdBy, createdDate. Sort sai trả về 400.")
    public ResponseEntity<PageResponse<ProductPriceResponse>> getPrices(
            @Parameter(description = "ID sản phẩm cần xem lịch sử giá") @RequestParam(required = false) Long productId,
            @Parameter(description = "Lọc cờ bản giá được chọn áp dụng (true) hoặc giá đã đóng (false)")
            @RequestParam(name = "isPresentPrice", required = false) Boolean presentPrice,
            @ParameterObject @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productPriceService.getPrices(productId, presentPrice, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết phiên bản giá", description = "Lấy phiên bản giá theo ID, bao gồm giá lịch sử. Trả về 404 nếu không tồn tại.")
    public ResponseEntity<ProductPriceResponse> getPriceById(@Parameter(description = "ID phiên bản giá") @PathVariable Long id) {
        return ResponseEntity.ok(productPriceService.getPriceById(id));
    }

}
