package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.InventoryAnalyticsService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryAnalyticsService inventoryService;

    @InjectMocks
    private InventoryAnalyticsController inventoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/inventory/summary - Thành công trả về tổng quan kho")
    void testGetInventorySummary() throws Exception {
        InventorySummaryDto summary = InventorySummaryDto.builder()
                .totalSerialsInStore(1250L)
                .totalInventoryValue(3_450_000_000L)
                .sellableSerials(1120L)
                .sellableValue(3_100_000_000L)
                .defectiveSerials(35L)
                .packingAndDeliveringSerials(65L)
                .technicalUsageSerials(30L)
                .build();

        when(inventoryService.getInventorySummary(any())).thenReturn(summary);

        mockMvc.perform(get("/api/analytics/inventory/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSerialsInStore").value(1250))
                .andExpect(jsonPath("$.sellableSerials").value(1120))
                .andExpect(jsonPath("$.sellableValue").value(3100000000L));
    }

    @Test
    @DisplayName("GET /api/analytics/inventory/by-category - Thành công trả về tồn theo danh mục")
    void testGetStockByCategory() throws Exception {
        List<CategoryBreakdownDto> list = List.of(
                CategoryBreakdownDto.builder().categoryId(1L).categoryName("VGA").value(150L).percentage(35.0).build()
        );

        when(inventoryService.getStockByCategory(any())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/inventory/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("VGA"))
                .andExpect(jsonPath("$[0].value").value(150));
    }

    @Test
    @DisplayName("GET /api/analytics/inventory/aging - Thành công trả về tháp tuổi tồn kho")
    void testGetInventoryAging() throws Exception {
        List<InventoryAgingBracketDto> list = List.of(
                InventoryAgingBracketDto.builder().bracket("DAYS_0_TO_30").label("0 - 30 ngày").quantity(780L).totalValue(2_150_000_000L).percentage(62.4).build()
        );

        when(inventoryService.getInventoryAging()).thenReturn(list);

        mockMvc.perform(get("/api/analytics/inventory/aging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bracket").value("DAYS_0_TO_30"))
                .andExpect(jsonPath("$[0].quantity").value(780));
    }

    @Test
    @DisplayName("GET /api/analytics/inventory/low-stock-alert - Thành công trả về danh sách sắp hết hàng")
    void testGetLowStockAlerts() throws Exception {
        List<LowStockProductDto> list = List.of(
                LowStockProductDto.builder()
                        .productId(105L)
                        .productCode("VGA-RTX4070")
                        .productName("ASUS RTX 4070")
                        .categoryName("VGA")
                        .sellableQuantity(2L)
                        .minimumThreshold(5)
                        .suggestedSupplier("Vĩnh Xuân")
                        .build()
        );

        when(inventoryService.getLowStockAlerts(any())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/inventory/low-stock-alert").param("minThreshold", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("VGA-RTX4070"))
                .andExpect(jsonPath("$[0].sellableQuantity").value(2));
    }

    @Test
    @DisplayName("GET /api/analytics/inventory/slow-moving - Thành công trả về hàng bán chậm")
    void testGetSlowMovingProducts() throws Exception {
        List<SlowMovingProductDto> list = List.of(
                SlowMovingProductDto.builder()
                        .productId(201L)
                        .productCode("CASE-NZXT-H510")
                        .productName("Vỏ case NZXT H510")
                        .categoryName("Case")
                        .sellableQuantity(8L)
                        .lastSoldDate(Instant.now())
                        .daysWithoutSale(60L)
                        .build()
        );

        when(inventoryService.getSlowMovingProducts(eq(45))).thenReturn(list);

        mockMvc.perform(get("/api/analytics/inventory/slow-moving").param("thresholdDays", "45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("CASE-NZXT-H510"))
                .andExpect(jsonPath("$[0].daysWithoutSale").value(60));
    }
}
