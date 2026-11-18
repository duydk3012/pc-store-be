package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.analytics.dto.response.ProductMatrixDto;
import com.datn.pc_store.modules.analytics.dto.response.TopProductDto;
import com.datn.pc_store.modules.analytics.service.ProductAnalyticsService;
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
class ProductAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductAnalyticsService productService;

    @InjectMocks
    private ProductAnalyticsController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/products/top-selling - Thành công trả về top bán chạy")
    void testGetTopSelling() throws Exception {
        List<TopProductDto> list = List.of(
                TopProductDto.builder()
                        .productId(204L)
                        .productCode("CPU-I5-14600K")
                        .productName("CPU Intel Core i5 14600K")
                        .categoryName("CPU")
                        .quantitySold(85L)
                        .revenue(595_000_000L)
                        .grossProfit(115_000_000L)
                        .grossMarginPercentage(19.33)
                        .build()
        );

        when(productService.getTopSellingProducts(any(), eq(10))).thenReturn(list);

        mockMvc.perform(get("/api/analytics/products/top-selling").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("CPU-I5-14600K"))
                .andExpect(jsonPath("$[0].quantitySold").value(85));
    }

    @Test
    @DisplayName("GET /api/analytics/products/top-profit - Thành công trả về top lợi nhuận gộp")
    void testGetTopProfit() throws Exception {
        List<TopProductDto> list = List.of(
                TopProductDto.builder()
                        .productId(101L)
                        .productCode("VGA-RTX4090")
                        .productName("ASUS ROG Strix RTX 4090")
                        .categoryName("VGA")
                        .revenue(900_000_000L)
                        .cogs(700_000_000L)
                        .grossProfit(200_000_000L)
                        .grossMarginPercentage(22.22)
                        .build()
        );

        when(productService.getTopGrossProfitProducts(any(), eq(10))).thenReturn(list);

        mockMvc.perform(get("/api/analytics/products/top-profit").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("VGA-RTX4090"))
                .andExpect(jsonPath("$[0].grossProfit").value(200000000L));
    }

    @Test
    @DisplayName("GET /api/analytics/products/performance-matrix - Thành công trả về ma trận hiệu quả SKU")
    void testGetPerformanceMatrix() throws Exception {
        List<ProductMatrixDto> list = List.of(
                ProductMatrixDto.builder()
                        .productId(204L)
                        .productCode("CPU-I5-14600K")
                        .productName("Intel Core i5 14600K")
                        .categoryName("CPU")
                        .brandName("Intel")
                        .quantitySold(85L)
                        .revenue(595_000_000L)
                        .cogs(480_000_000L)
                        .grossProfit(115_000_000L)
                        .currentStock(25L)
                        .warrantyCount(2L)
                        .build()
        );

        when(productService.getProductPerformanceMatrix(any())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/products/performance-matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("CPU-I5-14600K"))
                .andExpect(jsonPath("$[0].brandName").value("Intel"))
                .andExpect(jsonPath("$[0].currentStock").value(25))
                .andExpect(jsonPath("$[0].warrantyCount").value(2));
    }
}
