package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.DashboardAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/dashboard")
@RequiredArgsConstructor
@Tag(name = "Analytics - Dashboard", description = "Báo cáo Tổng quan hoạt động kinh doanh")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService dashboardService;

    @GetMapping("/kpi")
    @Operation(summary = "7 Thẻ KPI Tổng hợp", description = "Tổng doanh thu, Lượng hóa đơn, Sản phẩm đã bán, Lợi nhuận gộp, Giá trị tồn kho, Cảnh báo sắp hết hàng, Đơn đang xử lý")
    public ResponseEntity<DashboardKpiResponse> getDashboardKpis(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getDashboardKpis(filter));
    }

    @GetMapping("/revenue-order-trend")
    @Operation(summary = "DASH-CH-01: Biểu đồ Combo Doanh thu và Số đơn theo thời gian")
    public ResponseEntity<List<TimeSeriesPointDto>> getRevenueAndOrderTrend(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getRevenueAndOrderTrend(filter));
    }

    @GetMapping("/revenue-by-category")
    @Operation(summary = "DASH-CH-02: Cơ cấu doanh thu theo Danh mục chính (Donut Chart - Chuẩn Kế toán 100%)")
    public ResponseEntity<List<CategoryBreakdownDto>> getRevenueByCategory(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getRevenueByCategory(filter));
    }

    @GetMapping("/revenue-by-category/marketing")
    @Operation(summary = "DASH-CH-04: Doanh thu theo từng Danh mục / Bộ sưu tập (Bar Chart - Tính cả danh mục chính & phụ)")
    public ResponseEntity<List<CategoryBreakdownDto>> getRevenueByCategoryMarketing(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getRevenueByCategoryMarketing(filter));
    }

    @GetMapping("/order-status-distribution")
    @Operation(summary = "DASH-CH-03: Cơ cấu đơn hàng theo trạng thái hiện tại")
    public ResponseEntity<List<OrderStatusDistributionDto>> getOrderStatusDistribution() {
        return ResponseEntity.ok(dashboardService.getOrderStatusDistribution());
    }
}
