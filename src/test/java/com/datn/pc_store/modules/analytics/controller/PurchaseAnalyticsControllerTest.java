package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.service.PurchaseAnalyticsService;
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
class PurchaseAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PurchaseAnalyticsService purchaseService;

    @InjectMocks
    private PurchaseAnalyticsController purchaseController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(purchaseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/purchasing/trend - Thành công trả về giá trị nhập theo thời gian")
    void testGetPurchasingTrend() throws Exception {
        List<TimeSeriesPointDto> list = List.of(
                TimeSeriesPointDto.builder().timeBucket("2026-10-01").label("Tuần 40").revenue(450_000_000L).orderCount(3L).build()
        );

        when(purchaseService.getPurchasingTrend(any())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/purchasing/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revenue").value(450000000L))
                .andExpect(jsonPath("$[0].orderCount").value(3));
    }

    @Test
    @DisplayName("GET /api/analytics/purchasing/by-supplier - Thành công trả về giá trị nhập theo NCC")
    void testGetPurchasingBySupplier() throws Exception {
        List<SupplierPerformanceDto> list = List.of(
                SupplierPerformanceDto.builder()
                        .supplierId(1L)
                        .supplierName("Vĩnh Xuân")
                        .supplierCode("NCC-VX")
                        .totalImportValue(750_000_000L)
                        .receiptCount(5L)
                        .totalQuantityReceived(350L)
                        .acceptedQuantity(345L)
                        .defectiveQuantity(5L)
                        .defectRatePercentage(1.43)
                        .build()
        );

        when(purchaseService.getPurchasingBySupplier(any(), eq(10))).thenReturn(list);

        mockMvc.perform(get("/api/analytics/purchasing/by-supplier").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierName").value("Vĩnh Xuân"))
                .andExpect(jsonPath("$[0].totalImportValue").value(750000000L))
                .andExpect(jsonPath("$[0].defectRatePercentage").value(1.43));
    }

    @Test
    @DisplayName("GET /api/analytics/purchasing/quality-check - Thành công trả về tỷ lệ đạt và lỗi")
    void testGetQualityInspection() throws Exception {
        List<ReceiptQualityDto> list = List.of(
                ReceiptQualityDto.builder().label("Vĩnh Xuân").acceptedQuantity(490L).defectiveQuantity(10L).totalQuantity(500L).defectRatePercentage(2.0).build()
        );

        when(purchaseService.getQualityInspection(any())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/purchasing/quality-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acceptedQuantity").value(490))
                .andExpect(jsonPath("$[0].defectRatePercentage").value(2.0));
    }

    @Test
    @DisplayName("GET /api/analytics/purchasing/receipt-status-distribution - Thành công trả về trạng thái phiếu nhập")
    void testGetReceiptStatusDistribution() throws Exception {
        List<OrderStatusDistributionDto> list = List.of(
                OrderStatusDistributionDto.builder().status("CONFIRMED").label("Đã xác nhận").count(10L).percentage(83.33).build()
        );

        when(purchaseService.getReceiptStatusDistribution(any())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/purchasing/receipt-status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].count").value(10));
    }
}
