package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.TaxReportFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterDto;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterlyResponse;
import com.datn.pc_store.modules.analytics.dto.response.TaxSummaryResponse;
import com.datn.pc_store.modules.analytics.enums.TaxTierBracket;
import com.datn.pc_store.modules.analytics.repository.AnalyticsTaxRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxExcelExportServiceTest {

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private AnalyticsTaxRepository taxRepository;

    @InjectMocks
    private TaxExcelExportService taxExcelExportService;

    @Test
    @DisplayName("Kiểm tra xuất file Excel Báo cáo Hoạch toán Thuế gồm 2 Sheet hợp lệ")
    void testExportTaxReportToExcel() throws Exception {
        TaxReportFilterRequest filter = TaxReportFilterRequest.builder()
                .year(2026)
                .build();

        TaxSummaryResponse mockSummary = TaxSummaryResponse.builder()
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
                .totalPaidInvoices(10L)
                .generatedAt(Instant.now())
                .build();

        TaxQuarterlyResponse mockQuarterly = TaxQuarterlyResponse.builder()
                .taxYear(2026)
                .annualNetRevenue(4_250_000_000L)
                .appliedTaxRate(0.25)
                .quarters(List.of(
                        TaxQuarterDto.builder().quarter(1).quarterRevenue(1_000_000_000L).vatAmount(80_000_000L).pitAmount(170_000_000L).totalTax(250_000_000L).invoiceCount(2).build(),
                        TaxQuarterDto.builder().quarter(2).quarterRevenue(1_000_000_000L).vatAmount(80_000_000L).pitAmount(170_000_000L).totalTax(250_000_000L).invoiceCount(3).build(),
                        TaxQuarterDto.builder().quarter(3).quarterRevenue(1_000_000_000L).vatAmount(80_000_000L).pitAmount(170_000_000L).totalTax(250_000_000L).invoiceCount(2).build(),
                        TaxQuarterDto.builder().quarter(4).quarterRevenue(1_250_000_000L).vatAmount(100_000_000L).pitAmount(212_500_000L).totalTax(312_500_000L).invoiceCount(3).build()
                ))
                .build();

        Object[] invoiceRow = new Object[]{
                1L,
                "HD-20261101-0001",
                Timestamp.from(Instant.now()),
                "ORD-20261101-001",
                "Nguyễn Văn A",
                "0987654321",
                25_000_000L,
                1_000_000L,
                24_000_000L,
                2 // status = PAID
        };

        when(taxCalculationService.calculateTaxSummary(any())).thenReturn(mockSummary);
        when(taxCalculationService.getTaxQuarterlyBreakdown(2026)).thenReturn(mockQuarterly);
        when(taxRepository.queryAllTaxInvoicesForExport(any(), any())).thenReturn(Collections.singletonList(invoiceRow));

        byte[] excelBytes = taxExcelExportService.exportTaxReportToExcel(filter);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        // Đọc lại file Excel để kiểm tra số lượng Sheet và tiêu đề
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("Tờ Khai Thuế & Tổng Hợp", wb.getSheetAt(0).getSheetName());
            assertEquals("Bảng Kê Hóa Đơn Bán Ra", wb.getSheetAt(1).getSheetName());
        }
    }
}
