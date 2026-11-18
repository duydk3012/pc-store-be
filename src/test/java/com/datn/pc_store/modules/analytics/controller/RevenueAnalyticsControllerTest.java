package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.RevenueAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RevenueAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RevenueAnalyticsService revenueService;

    @InjectMocks
    private RevenueAnalyticsController revenueController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(revenueController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/revenue/trend - Thành công trả về xu hướng doanh thu và lợi nhuận")
    void testGetRevenueAndProfitTrend() throws Exception {
        List<TimeSeriesPointDto> points = List.of(
                TimeSeriesPointDto.builder().timeBucket("2026-10-01").label("01/10").revenue(50_000_000L).cogs(38_000_000L).grossProfit(12_000_000L).orderCount(6L).build()
        );

        when(revenueService.getRevenueAndProfitTrend(any())).thenReturn(points);

        mockMvc.perform(get("/api/analytics/revenue/trend")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revenue").value(50000000L))
                .andExpect(jsonPath("$[0].cogs").value(38000000L))
                .andExpect(jsonPath("$[0].grossProfit").value(12000000L));
    }

    @Test
    @DisplayName("GET /api/analytics/revenue/compare - Thành công đối chiếu kỳ trước")
    void testCompareRevenue() throws Exception {
        PeriodComparisonDto comparison = PeriodComparisonDto.builder()
                .currentRevenue(850_000_000L)
                .comparedRevenue(720_000_000L)
                .growthAmount(130_000_000L)
                .growthRatePercentage(18.06)
                .currentPeriodLabel("10/2026")
                .comparedPeriodLabel("09/2026")
                .build();

        when(revenueService.compareRevenueBetweenPeriods(any())).thenReturn(comparison);

        mockMvc.perform(get("/api/analytics/revenue/compare")
                        .param("compareWith", "PREVIOUS_PERIOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRevenue").value(850000000L))
                .andExpect(jsonPath("$.comparedRevenue").value(720000000L))
                .andExpect(jsonPath("$.growthRatePercentage").value(18.06));
    }

    @Test
    @DisplayName("GET /api/analytics/revenue/by-brand - Thành công trả về doanh thu theo thương hiệu")
    void testGetRevenueByBrand() throws Exception {
        List<BrandBreakdownDto> brands = List.of(
                BrandBreakdownDto.builder().brandId(1L).brandName("ASUS").revenue(450_000_000L).percentage(50.0).build(),
                BrandBreakdownDto.builder().brandId(2L).brandName("MSI").revenue(250_000_000L).percentage(27.78).build()
        );

        when(revenueService.getRevenueByBrand(any(), eq(10))).thenReturn(brands);

        mockMvc.perform(get("/api/analytics/revenue/by-brand").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brandName").value("ASUS"))
                .andExpect(jsonPath("$[0].revenue").value(450000000L));
    }

    @Test
    @DisplayName("GET /api/analytics/revenue/by-payment-method - Thành công trả về cơ cấu thanh toán")
    void testGetRevenueByPaymentMethod() throws Exception {
        List<PaymentMethodBreakdownDto> methods = List.of(
                PaymentMethodBreakdownDto.builder().methodId(1L).methodName("Chuyển khoản").amount(600_000_000L).percentage(75.0).build()
        );

        when(revenueService.getRevenueByPaymentMethod(any())).thenReturn(methods);

        mockMvc.perform(get("/api/analytics/revenue/by-payment-method"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].methodName").value("Chuyển khoản"))
                .andExpect(jsonPath("$[0].amount").value(600000000L));
    }
}
