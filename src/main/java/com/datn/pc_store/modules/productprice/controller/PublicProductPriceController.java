package com.datn.pc_store.modules.productprice.controller;

import com.datn.pc_store.modules.productprice.dto.ProductPriceResponse;
import com.datn.pc_store.modules.productprice.service.ProductPriceService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Product Prices (Public)", description = "Tra cứu giá bán đang hiệu lực của sản phẩm công khai")
public class PublicProductPriceController {
    private final ProductPriceService productPriceService;

    @GetMapping("/{productId}/price")
    @Operation(summary = "Xem giá bán hiện tại", description = "Không yêu cầu đăng nhập. Chỉ trả giá đang hiệu lực của sản phẩm ACTIVE hoặc OUT_OF_STOCK chưa bị xóa. Trả về 404 nếu sản phẩm bị ẩn/xóa hoặc chưa có giá hiệu lực; không công khai lịch sử giá.")
    public ResponseEntity<ProductPriceResponse> getCurrentPrice(@Parameter(description = "ID sản phẩm") @PathVariable Long productId) {
        return ResponseEntity.ok(productPriceService.getCurrentPrice(productId, true));
    }
}
