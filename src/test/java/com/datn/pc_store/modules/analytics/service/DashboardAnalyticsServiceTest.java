package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.CategoryBreakdownDto;
import com.datn.pc_store.modules.analytics.repository.AnalyticsDashboardRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsServiceTest {

    @Mock
    private AnalyticsDashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardAnalyticsService dashboardService;

    @Test
    @DisplayName("getRevenueByCategory - Bậc cha gom nhóm doanh thu từ bậc con & tính % chi tiết từng con + Khác trên tổng cha")
    void testGetRevenueByCategoryHierarchicalBreakdown() {
        // Giả lập dữ liệu từ queryRevenueByCategoryHierarchy:
        // - Linh kiện (10001): SSD (10005) = 2,000; trực thuộc Linh kiện không có con (null) = 1,000 => Tổng cha = 3,000 (75%)
        // - Sản phẩm bán chạy (10099): trực thuộc không có con (null) = 1,000 => Tổng cha = 1,000 (25%)
        List<Object[]> mockRows = List.of(
                new Object[]{10001L, "Linh kiện", 10005L, "SSD", 2_000L},
                new Object[]{10001L, "Linh kiện", null, null, 1_000L},
                new Object[]{10099L, "Sản phẩm bán chạy", null, null, 1_000L}
        );

        when(dashboardRepository.queryRevenueByCategoryHierarchy(any(), any()))
                .thenReturn(mockRows);

        LocalDate today = LocalDate.now(AnalyticsDateHelper.VN_ZONE);
        DateRangeFilterRequest filter = DateRangeFilterRequest.builder()
                .startDate(today.minusDays(30))
                .endDate(today)
                .build();

        List<CategoryBreakdownDto> result = dashboardService.getRevenueByCategory(filter);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Kiểm tra cấp cha 1: Linh kiện
        CategoryBreakdownDto linhKien = result.get(0);
        assertEquals(10001L, linhKien.getCategoryId());
        assertEquals("Linh kiện", linhKien.getCategoryName());
        assertEquals(3_000L, linhKien.getValue());
        assertEquals(75.0, linhKien.getPercentage(), 0.01);

        // Kiểm tra danh sách con của Linh kiện (tính % trên tổng cha 3,000)
        assertNotNull(linhKien.getChildren());
        assertEquals(2, linhKien.getChildren().size());

        CategoryBreakdownDto ssd = linhKien.getChildren().get(0);
        assertEquals(10005L, ssd.getCategoryId());
        assertEquals("SSD", ssd.getCategoryName());
        assertEquals(2_000L, ssd.getValue());
        assertEquals(66.67, ssd.getPercentage(), 0.01);

        CategoryBreakdownDto khac = linhKien.getChildren().get(1);
        assertEquals(0L, khac.getCategoryId());
        assertEquals("Khác", khac.getCategoryName());
        assertEquals(1_000L, khac.getValue());
        assertEquals(33.33, khac.getPercentage(), 0.01);

        assertEquals(100.0, ssd.getPercentage() + khac.getPercentage(), 0.01);

        // Kiểm tra cấp cha 2: Sản phẩm bán chạy (không có con -> children rỗng)
        CategoryBreakdownDto bestSeller = result.get(1);
        assertEquals(10099L, bestSeller.getCategoryId());
        assertEquals("Sản phẩm bán chạy", bestSeller.getCategoryName());
        assertEquals(1_000L, bestSeller.getValue());
        assertEquals(25.0, bestSeller.getPercentage(), 0.01);
        assertTrue(bestSeller.getChildren().isEmpty());

        // Tổng tỷ trọng cấp cha = 100%
        assertEquals(100.0, linhKien.getPercentage() + bestSeller.getPercentage(), 0.01);
    }

    @Test
    @DisplayName("getRevenueByCategoryMarketing - Tính cả danh mục chính & phụ (Bar Chart), con tính % trên tổng nhánh cha")
    void testGetRevenueByCategoryMarketingAllLinked() {
        // Ví dụ 4 sản phẩm (tổng doanh thu thực tế toàn cửa hàng = 4,000):
        // - Linh kiện (chính + phụ): SSD=2,000, Mainboard=1,000, Khác=1,000 => Tổng Linh kiện = 4,000 (100% doanh thu cửa hàng)
        // - Sản phẩm bán chạy (chính + phụ): 2,000 (50% doanh thu cửa hàng)
        List<Object[]> mockRows = List.of(
                new Object[]{10001L, "Linh kiện", 10005L, "SSD", 2_000L},
                new Object[]{10001L, "Linh kiện", 10003L, "Mainboard", 1_000L},
                new Object[]{10001L, "Linh kiện", null, null, 1_000L},
                new Object[]{10099L, "Sản phẩm bán chạy", null, null, 2_000L}
        );

        when(dashboardRepository.queryRevenueByAllLinkedCategories(any(), any()))
                .thenReturn(mockRows);
        when(dashboardRepository.sumRevenueBetween(any(), any()))
                .thenReturn(4_000L);

        LocalDate today = LocalDate.now(AnalyticsDateHelper.VN_ZONE);
        DateRangeFilterRequest filter = DateRangeFilterRequest.builder()
                .startDate(today.minusDays(30))
                .endDate(today)
                .build();

        List<CategoryBreakdownDto> result = dashboardService.getRevenueByCategoryMarketing(filter);

        assertEquals(2, result.size());

        CategoryBreakdownDto linhKien = result.get(0);
        assertEquals("Linh kiện", linhKien.getCategoryName());
        assertEquals(4_000L, linhKien.getValue());
        assertEquals(100.0, linhKien.getPercentage(), 0.01);
        assertEquals(3, linhKien.getChildren().size());

        assertEquals("SSD", linhKien.getChildren().get(0).getCategoryName());
        assertEquals(50.0, linhKien.getChildren().get(0).getPercentage(), 0.01);
        assertEquals("Mainboard", linhKien.getChildren().get(1).getCategoryName());
        assertEquals(25.0, linhKien.getChildren().get(1).getPercentage(), 0.01);
        assertEquals("Khác", linhKien.getChildren().get(2).getCategoryName());
        assertEquals(25.0, linhKien.getChildren().get(2).getPercentage(), 0.01);

        CategoryBreakdownDto bestSeller = result.get(1);
        assertEquals("Sản phẩm bán chạy", bestSeller.getCategoryName());
        assertEquals(2_000L, bestSeller.getValue());
        assertEquals(50.0, bestSeller.getPercentage(), 0.01);
        assertTrue(bestSeller.getChildren().isEmpty());
    }
}
