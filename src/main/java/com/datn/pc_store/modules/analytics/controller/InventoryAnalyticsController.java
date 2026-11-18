package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.modules.analytics.dto.request.InventoryFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.InventoryAnalyticsService;
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
@RequestMapping("/api/analytics/inventory")
@RequiredArgsConstructor
@Tag(name = "Analytics - Inventory", description = "Báo cáo Quản lý & Phân tích Tồn kho")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF_WAREHOUSE')")
public class InventoryAnalyticsController {

    private final InventoryAnalyticsService inventoryService;

    @GetMapping("/summary")
    @Operation(summary = "Tổng quan số lượng và giá trị vốn kho")
    public ResponseEntity<InventorySummaryDto> getInventorySummary(@ParameterObject InventoryFilterRequest filter) {
        return ResponseEntity.ok(inventoryService.getInventorySummary(filter));
    }

    @GetMapping("/by-category")
    @Operation(summary = "INV-CH-01 & 02: Tồn kho và giá trị vốn theo danh mục")
    public ResponseEntity<List<CategoryBreakdownDto>> getStockByCategory(@ParameterObject InventoryFilterRequest filter) {
        return ResponseEntity.ok(inventoryService.getStockByCategory(filter));
    }

    @GetMapping("/status-distribution")
    @Operation(summary = "INV-CH-03: Cơ cấu phân bổ trạng thái Serial kho")
    public ResponseEntity<List<OrderStatusDistributionDto>> getSerialStatusDistribution() {
        return ResponseEntity.ok(inventoryService.getSerialStatusDistribution());
    }

    @GetMapping("/aging")
    @Operation(summary = "INV-CH-05: Tháp tuổi tồn kho (Aging: 0-30, 31-60, 61-90, >90 ngày)")
    public ResponseEntity<List<InventoryAgingBracketDto>> getInventoryAging() {
        return ResponseEntity.ok(inventoryService.getInventoryAging());
    }

    @GetMapping("/low-stock-alert")
    @Operation(summary = "INV-TB-06: Danh sách sản phẩm cảnh báo sắp hết hàng")
    public ResponseEntity<List<LowStockProductDto>> getLowStockAlerts(@ParameterObject InventoryFilterRequest filter) {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts(filter));
    }

    @GetMapping("/slow-moving")
    @Operation(summary = "INV-TB-07: Danh sách sản phẩm bán chậm hoặc tồn đọng lâu")
    public ResponseEntity<List<SlowMovingProductDto>> getSlowMovingProducts(
            @Parameter(description = "Ngưỡng số ngày không phát sinh bán (mặc định 30 ngày)")
            @RequestParam(defaultValue = "30") int thresholdDays) {
        return ResponseEntity.ok(inventoryService.getSlowMovingProducts(thresholdDays));
    }
}
