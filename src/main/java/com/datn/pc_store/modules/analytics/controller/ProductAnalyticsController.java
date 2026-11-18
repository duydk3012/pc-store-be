package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.ProductMatrixDto;
import com.datn.pc_store.modules.analytics.dto.response.TopProductDto;
import com.datn.pc_store.modules.analytics.service.ProductAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/products")
@RequiredArgsConstructor
@Tag(name = "Analytics - Products", description = "Báo cáo Phân tích Hiệu quả Sản phẩm")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF_SALES')")
public class ProductAnalyticsController {

    private final ProductAnalyticsService productService;

    @GetMapping("/top-selling")
    @Operation(summary = "PRD-CH-01: Top 10 sản phẩm bán chạy nhất theo sản lượng")
    public ResponseEntity<List<TopProductDto>> getTopSelling(
            @ParameterObject DateRangeFilterRequest filter,
            @Parameter(description = "Số lượng sản phẩm lấy (mặc định 10)")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getTopSellingProducts(filter, limit));
    }

    @GetMapping("/top-profit")
    @Operation(summary = "PRD-CH-02: Top 10 sản phẩm sinh lời gộp cao nhất")
    public ResponseEntity<List<TopProductDto>> getTopProfit(
            @ParameterObject DateRangeFilterRequest filter,
            @Parameter(description = "Số lượng sản phẩm lấy (mặc định 10)")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getTopGrossProfitProducts(filter, limit));
    }

    @GetMapping("/performance-matrix")
    @Operation(summary = "PRD-TB-04: Bảng ma trận hiệu quả chi tiết từng SKU (Sản lượng, Doanh thu, COGS, Lợi nhuận, Tồn kho, Bảo hành)")
    public ResponseEntity<List<ProductMatrixDto>> getPerformanceMatrix(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(productService.getProductPerformanceMatrix(filter));
    }
}
