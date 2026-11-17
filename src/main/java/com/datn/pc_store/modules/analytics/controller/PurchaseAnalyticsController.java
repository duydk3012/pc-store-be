package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.modules.analytics.dto.request.PurchaseFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.PurchaseAnalyticsService;
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
@RequestMapping("/api/analytics/purchasing")
@RequiredArgsConstructor
@Tag(name = "Analytics - Purchasing", description = "Báo cáo Quản lý Nhập hàng & Nhà cung cấp")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF_WAREHOUSE')")
public class PurchaseAnalyticsController {

    private final PurchaseAnalyticsService purchaseService;

    @GetMapping("/trend")
    @Operation(summary = "PUR-CH-01: Giá trị nhập hàng thực tế theo thời gian")
    public ResponseEntity<List<TimeSeriesPointDto>> getPurchasingTrend(@ParameterObject PurchaseFilterRequest filter) {
        return ResponseEntity.ok(purchaseService.getPurchasingTrend(filter));
    }

    @GetMapping("/by-supplier")
    @Operation(summary = "PUR-CH-02: Top nhà cung cấp theo giá trị nhập")
    public ResponseEntity<List<SupplierPerformanceDto>> getPurchasingBySupplier(
            @ParameterObject PurchaseFilterRequest filter,
            @Parameter(description = "Số lượng nhà cung cấp lấy (mặc định 10)")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(purchaseService.getPurchasingBySupplier(filter, limit));
    }

    @GetMapping("/quality-check")
    @Operation(summary = "PUR-CH-03: Đánh giá chất lượng kiểm hàng Đạt vs Lỗi")
    public ResponseEntity<List<ReceiptQualityDto>> getQualityInspection(@ParameterObject PurchaseFilterRequest filter) {
        return ResponseEntity.ok(purchaseService.getQualityInspection(filter));
    }

    @GetMapping("/receipt-status-distribution")
    @Operation(summary = "PUR-CH-04: Cơ cấu phiếu nhập theo trạng thái chứng từ")
    public ResponseEntity<List<OrderStatusDistributionDto>> getReceiptStatusDistribution(@ParameterObject PurchaseFilterRequest filter) {
        return ResponseEntity.ok(purchaseService.getReceiptStatusDistribution(filter));
    }
}
