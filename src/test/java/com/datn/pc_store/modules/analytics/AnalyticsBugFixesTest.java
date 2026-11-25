package com.datn.pc_store.modules.analytics;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.modules.analytics.dto.request.TaxReportFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterDto;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterlyResponse;
import com.datn.pc_store.modules.analytics.dto.response.TaxSummaryResponse;
import com.datn.pc_store.modules.analytics.enums.TaxTierBracket;
import com.datn.pc_store.modules.analytics.repository.AnalyticsTaxRepository;
import com.datn.pc_store.modules.analytics.service.TaxCalculationService;
import com.datn.pc_store.modules.analytics.service.TaxExcelExportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsBugFixesTest {

    @Mock
    private AnalyticsTaxRepository taxRepository;

    @InjectMocks
    private TaxCalculationService taxCalculationService;

    @Mock
    private TaxCalculationService mockTaxCalculationService;

    @Test
    @DisplayName("TaxCalculationService từ chối truyền đồng thời cả month và quarter")
    void rejectBothMonthAndQuarter() {
        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .quarter(1)
                .month(2)
                .build();

        assertThrows(BadRequestException.class, () -> taxCalculationService.calculateTaxSummary(request));
    }

    @Test
    @DisplayName("TaxCalculationService áp dụng đúng khoảng thời gian tháng (VN +07:00) khi lọc theo month")
    void resolveMonthRangeCorrectly() {
        when(taxRepository.queryRevenueAndCountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(new Object[]{500_000_000L, 12L}));

        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .month(2)
                .build();

        TaxSummaryResponse summary = taxCalculationService.calculateTaxSummary(request);

        ArgumentCaptor<Instant> fromCap = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCap = ArgumentCaptor.forClass(Instant.class);
        verify(taxRepository, times(2)).queryRevenueAndCountBetween(fromCap.capture(), toCap.capture());

        // 2026-02-01T00:00:00+07:00 = 2026-01-31T17:00:00Z
        assertTrue(fromCap.getAllValues().contains(Instant.parse("2026-01-31T17:00:00Z")));
        // 2026-02-28T23:59:59.999999999+07:00 = 2026-02-28T16:59:59.999999999Z
        assertTrue(toCap.getAllValues().contains(Instant.parse("2026-02-28T16:59:59.999999999Z")));
        assertEquals(12L, summary.getTotalPaidInvoices());
    }

    @Test
    @DisplayName("TaxExcelExportService chống Formula Injection trên tên khách hàng và mã đơn/hóa đơn")
    void exportExcelSanitizesFormulaInjection() throws Exception {
        TaxExcelExportService exportService = new TaxExcelExportService(mockTaxCalculationService, taxRepository);

        TaxSummaryResponse mockSummary = TaxSummaryResponse.builder()
                .taxYear(2026)
                .annualNetRevenue(100_000_000L)
                .appliedTier(TaxTierBracket.UNDER_1B)
                .tierDescription("Dưới 1 tỷ")
                .vatRate(0.0).vatAmount(0L)
                .pitRate(0.0).pitAmount(0L)
                .totalTaxRate(0.0).totalTaxAmount(0L)
                .totalPaidInvoices(1L)
                .generatedAt(Instant.now())
                .build();

        TaxQuarterlyResponse mockQuarterly = TaxQuarterlyResponse.builder()
                .taxYear(2026)
                .annualNetRevenue(100_000_000L)
                .appliedTaxRate(0.0)
                .quarters(List.of(
                        TaxQuarterDto.builder().quarter(1).quarterRevenue(100_000_000L).vatAmount(0).pitAmount(0).totalTax(0).invoiceCount(1).build()
                ))
                .build();

        Object[] dangerousRow = new Object[]{
                1L,
                "=CMD|' /C calc'!A0",
                Timestamp.from(Instant.now()),
                "+ORD-001",
                "@EvilUser",
                "-0987654321",
                10_000_000L,
                0L,
                10_000_000L,
                2
        };

        when(mockTaxCalculationService.calculateTaxSummary(any())).thenReturn(mockSummary);
        when(mockTaxCalculationService.getTaxQuarterlyBreakdown(2026)).thenReturn(mockQuarterly);
        when(taxRepository.queryAllTaxInvoicesForExport(any(), any())).thenReturn(Collections.singletonList(dangerousRow));

        byte[] bytes = exportService.exportTaxReportToExcel(TaxReportFilterRequest.builder().year(2026).build());

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(1);
            Row dataRow = sheet.getRow(3);
            assertEquals("'=CMD|' /C calc'!A0", dataRow.getCell(1).getStringCellValue());
            assertEquals("'+ORD-001", dataRow.getCell(3).getStringCellValue());
            assertEquals("'@EvilUser", dataRow.getCell(4).getStringCellValue());
            assertEquals("'-0987654321", dataRow.getCell(5).getStringCellValue());
        }
    }
}
