package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.enums.TaxTierBracket;
import com.datn.pc_store.modules.analytics.service.TaxCalculationService;
import com.datn.pc_store.modules.analytics.service.TaxExcelExportService;
import com.datn.pc_store.modules.order.enums.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
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
class TaxReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private TaxExcelExportService taxExcelExportService;

    @InjectMocks
    private TaxReportController taxReportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taxReportController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/tax/summary - Thành công tính toán thuế năm")
    void testGetTaxSummary() throws Exception {
        TaxSummaryResponse mockResponse = TaxSummaryResponse.builder()
                .taxYear(2026)
                .annualNetRevenue(4_250_000_000L)
                .appliedTier(TaxTierBracket.FROM_3B_TO_50B)
                .tierDescription("Doanh thu từ trên 3 tỷ đến 50 tỷ trên năm")
                .vatRate(0.08)
                .vatAmount(340_000_000L)
                .pitRate(0.17)
                .pitAmount(722_500_000L)
                .totalTaxRate(0.25)
                .totalTaxAmount(1_062_500_000L)
                .totalPaidInvoices(1420L)
                .generatedAt(Instant.now())
                .build();

        when(taxCalculationService.calculateTaxSummary(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/tax/summary")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.annualNetRevenue").value(4250000000L))
                .andExpect(jsonPath("$.appliedTier").value("FROM_3B_TO_50B"))
                .andExpect(jsonPath("$.vatRate").value(0.08))
                .andExpect(jsonPath("$.vatAmount").value(340000000L))
                .andExpect(jsonPath("$.pitRate").value(0.17))
                .andExpect(jsonPath("$.pitAmount").value(722500000L))
                .andExpect(jsonPath("$.totalTaxAmount").value(1062500000L));
    }

    @Test
    @DisplayName("GET /api/analytics/tax/summary - Lỗi 400 khi thiếu hoặc sai năm tính thuế")
    void testGetTaxSummaryValidationFail() throws Exception {
        mockMvc.perform(get("/api/analytics/tax/summary")
                        .param("year", "2010")) // min 2020
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/analytics/tax/quarterly-breakdown - Thành công trả về 4 quý")
    void testGetTaxQuarterlyBreakdown() throws Exception {
        TaxQuarterlyResponse mockResponse = TaxQuarterlyResponse.builder()
                .taxYear(2026)
                .annualNetRevenue(4_250_000_000L)
                .appliedTaxRate(0.25)
                .quarters(List.of(
                        TaxQuarterDto.builder().quarter(1).quarterRevenue(950_000_000L).vatAmount(76_000_000L).pitAmount(161_500_000L).totalTax(237_500_000L).invoiceCount(310L).build(),
                        TaxQuarterDto.builder().quarter(2).quarterRevenue(1_100_000_000L).vatAmount(88_000_000L).pitAmount(187_000_000L).totalTax(275_000_000L).invoiceCount(380L).build()
                ))
                .build();

        when(taxCalculationService.getTaxQuarterlyBreakdown(eq(2026))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/tax/quarterly-breakdown")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.quarters.length()").value(2))
                .andExpect(jsonPath("$.quarters[0].quarter").value(1))
                .andExpect(jsonPath("$.quarters[0].quarterRevenue").value(950000000L));
    }

    @Test
    @DisplayName("GET /api/analytics/tax/invoices - Thành công trả về danh sách hóa đơn phân trang")
    void testGetTaxInvoices() throws Exception {
        List<TaxInvoiceDetailDto> invoices = List.of(
                TaxInvoiceDetailDto.builder()
                        .invoiceId(1L)
                        .invoiceCode("HD-20261101-001")
                        .invoiceDate(Instant.now())
                        .orderCode("ORD-001")
                        .customerName("Nguyễn Văn A")
                        .customerPhone("0987654321")
                        .totalAmount(25_000_000L)
                        .discountAmount(1_000_000L)
                        .amount(24_000_000L)
                        .vatAmount(1_920_000L)
                        .pitAmount(4_080_000L)
                        .status(InvoiceStatus.PAID)
                        .build()
        );

        PageResponse<TaxInvoiceDetailDto> pageResponse = PageResponse.of(new PageImpl<>(invoices, PageRequest.of(0, 20), 1));

        when(taxCalculationService.getTaxInvoices(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/analytics/tax/invoices")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].invoiceCode").value("HD-20261101-001"))
                .andExpect(jsonPath("$.content[0].amount").value(24000000L));
    }

    @Test
    @DisplayName("GET /api/analytics/tax/export/excel - Thành công trả về file Excel nhị phân")
    void testExportTaxExcel() throws Exception {
        byte[] fakeExcelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK ZIP header

        when(taxExcelExportService.exportTaxReportToExcel(any())).thenReturn(fakeExcelBytes);

        mockMvc.perform(get("/api/analytics/tax/export/excel")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bao_Cao_Hoach_Toan_Thue_2026.xlsx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(fakeExcelBytes));
    }
}
