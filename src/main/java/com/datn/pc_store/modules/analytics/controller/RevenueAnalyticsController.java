package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.RevenueAnalyticsService;
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
@RequestMapping("/api/analytics/revenue")
@RequiredArgsConstructor
@Tag(name = "Analytics - Revenue", description = "Báo cáo Phân tích Doanh thu & Lợi nhuận gộp")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class RevenueAnalyticsController {

    private final RevenueAnalyticsService revenueService;

    @GetMapping("/trend")
    @Operation(summary = "REV-CH-01: Xu hướng Doanh thu thuần, Giá vốn (COGS) và Lợi nhuận gộp")
    public ResponseEntity<List<TimeSeriesPointDto>> getRevenueAndProfitTrend(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(revenueService.getRevenueAndProfitTrend(filter));
    }

    @GetMapping("/compare")
    @Operation(summary = "REV-CH-02: Đối chiếu doanh thu đa kỳ", description = "So sánh với kỳ trước liền kề hoặc cùng kỳ năm trước")
    public ResponseEntity<PeriodComparisonDto> compareRevenue(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(revenueService.compareRevenueBetweenPeriods(filter));
    }

    @GetMapping("/by-brand")
    @Operation(summary = "REV-CH-04: Cơ cấu doanh thu theo Thương hiệu linh kiện")
    public ResponseEntity<List<BrandBreakdownDto>> getRevenueByBrand(
            @ParameterObject DateRangeFilterRequest filter,
            @Parameter(description = "Số lượng thương hiệu lấy (mặc định 10)")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(revenueService.getRevenueByBrand(filter, limit));
    }

    @GetMapping("/by-payment-method")
    @Operation(summary = "REV-CH-06: Cơ cấu theo Phương thức thanh toán")
    public ResponseEntity<List<PaymentMethodBreakdownDto>> getRevenueByPaymentMethod(@ParameterObject DateRangeFilterRequest filter) {
        return ResponseEntity.ok(revenueService.getRevenueByPaymentMethod(filter));
    }
}
