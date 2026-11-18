package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.DashboardAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardAnalyticsService dashboardService;

    @InjectMocks
    private DashboardAnalyticsController dashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/dashboard/kpi - Thành công trả về 7 thẻ KPI")
    void testGetDashboardKpisSuccess() throws Exception {
        DashboardKpiResponse mockResponse = DashboardKpiResponse.builder()
                .totalRevenue(2_450_000_000L)
                .totalPaidInvoices(320L)
                .totalProductsSold(415L)
                .grossProfit(588_000_000L)
                .grossMarginPercentage(24.0)
                .totalInventoryValue(1_250_000_000L)
                .lowStockProductCount(8L)
                .processingOrderCount(15L)
                .asOfDate(Instant.now())
                .build();

        when(dashboardService.getDashboardKpis(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/dashboard/kpi")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(2450000000L))
                .andExpect(jsonPath("$.totalPaidInvoices").value(320))
                .andExpect(jsonPath("$.grossProfit").value(588000000L))
                .andExpect(jsonPath("$.grossMarginPercentage").value(24.0))
                .andExpect(jsonPath("$.lowStockProductCount").value(8));
    }

    @Test
    @DisplayName("GET /api/analytics/dashboard/kpi - Lỗi 400 khi startDate > endDate")
    void testGetDashboardKpisInvalidDateRange() throws Exception {
        when(dashboardService.getDashboardKpis(any()))
                .thenThrow(new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc."));

        mockMvc.perform(get("/api/analytics/dashboard/kpi")
                        .param("startDate", "2026-11-15")
                        .param("endDate", "2026-10-15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/analytics/dashboard/revenue-order-trend - Thành công trả về chuỗi thời gian")
    void testGetRevenueAndOrderTrend() throws Exception {
        List<TimeSeriesPointDto> points = List.of(
                TimeSeriesPointDto.builder().timeBucket("2026-11-01").label("01/11").revenue(85_000_000L).orderCount(12L).build(),
                TimeSeriesPointDto.builder().timeBucket("2026-11-02").label("02/11").revenue(112_000_000L).orderCount(18L).build()
        );

        when(dashboardService.getRevenueAndOrderTrend(any())).thenReturn(points);

        mockMvc.perform(get("/api/analytics/dashboard/revenue-order-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].timeBucket").value("2026-11-01"))
                .andExpect(jsonPath("$[0].revenue").value(85000000L))
                .andExpect(jsonPath("$[1].orderCount").value(18));
    }

    @Test
    @DisplayName("GET /api/analytics/dashboard/revenue-by-category - Thành công trả về Top danh mục")
    void testGetRevenueByCategory() throws Exception {
        List<CategoryBreakdownDto> categories = List.of(
                CategoryBreakdownDto.builder().categoryId(1L).categoryName("VGA").value(450_000_000L).percentage(45.0).build(),
                CategoryBreakdownDto.builder().categoryId(2L).categoryName("CPU").value(300_000_000L).percentage(30.0).build()
        );

        when(dashboardService.getRevenueByCategory(any())).thenReturn(categories);

        mockMvc.perform(get("/api/analytics/dashboard/revenue-by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("VGA"))
                .andExpect(jsonPath("$[0].percentage").value(45.0));
    }

    @Test
    @DisplayName("GET /api/analytics/dashboard/order-status-distribution - Thành công trả về phân bổ trạng thái đơn")
    void testGetOrderStatusDistribution() throws Exception {
        List<OrderStatusDistributionDto> dist = List.of(
                OrderStatusDistributionDto.builder().status("COMPLETED").label("Hoàn tất").count(70L).percentage(70.0).build(),
                OrderStatusDistributionDto.builder().status("DELIVERING").label("Đang giao").count(15L).percentage(15.0).build()
        );

        when(dashboardService.getOrderStatusDistribution()).thenReturn(dist);

        mockMvc.perform(get("/api/analytics/dashboard/order-status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].count").value(70));
    }
}
