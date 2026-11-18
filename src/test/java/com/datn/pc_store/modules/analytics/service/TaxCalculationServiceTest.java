package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.TaxReportFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.TaxSummaryResponse;
import com.datn.pc_store.modules.analytics.enums.TaxTierBracket;
import com.datn.pc_store.modules.analytics.repository.AnalyticsTaxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCalculationServiceTest {

    @Mock
    private AnalyticsTaxRepository taxRepository;

    @InjectMocks
    private TaxCalculationService taxCalculationService;

    @Test
    @DisplayName("Kiểm tra 4 khung bậc thuế theo doanh thu năm")
    void testTaxTierBracketThresholds() {
        // Bậc 1: Dưới 1 tỷ
        assertEquals(TaxTierBracket.UNDER_1B, TaxTierBracket.determineTier(500_000_000L));
        assertEquals(TaxTierBracket.UNDER_1B, TaxTierBracket.determineTier(999_999_999L));
        assertEquals(0.0, TaxTierBracket.UNDER_1B.getVatRate());
        assertEquals(0.0, TaxTierBracket.UNDER_1B.getPitRate());
        assertEquals(0.0, TaxTierBracket.UNDER_1B.getTotalDirectRate());

        // Bậc 2: Từ 1 tỷ đến 3 tỷ
        assertEquals(TaxTierBracket.FROM_1B_TO_3B, TaxTierBracket.determineTier(1_000_000_000L));
        assertEquals(TaxTierBracket.FROM_1B_TO_3B, TaxTierBracket.determineTier(2_000_000_000L));
        assertEquals(TaxTierBracket.FROM_1B_TO_3B, TaxTierBracket.determineTier(3_000_000_000L));
        assertEquals(0.01, TaxTierBracket.FROM_1B_TO_3B.getVatRate());
        assertEquals(0.005, TaxTierBracket.FROM_1B_TO_3B.getPitRate());
        assertEquals(0.015, TaxTierBracket.FROM_1B_TO_3B.getTotalDirectRate());

        // Bậc 3: Từ trên 3 tỷ đến 50 tỷ
        assertEquals(TaxTierBracket.FROM_3B_TO_50B, TaxTierBracket.determineTier(3_000_000_001L));
        assertEquals(TaxTierBracket.FROM_3B_TO_50B, TaxTierBracket.determineTier(10_000_000_000L));
        assertEquals(TaxTierBracket.FROM_3B_TO_50B, TaxTierBracket.determineTier(50_000_000_000L));
        assertEquals(0.08, TaxTierBracket.FROM_3B_TO_50B.getVatRate());
        assertEquals(0.17, TaxTierBracket.FROM_3B_TO_50B.getPitRate());
        assertEquals(0.25, TaxTierBracket.FROM_3B_TO_50B.getTotalDirectRate());

        // Bậc 4: Trên 50 tỷ
        assertEquals(TaxTierBracket.OVER_50B, TaxTierBracket.determineTier(50_000_000_001L));
        assertEquals(TaxTierBracket.OVER_50B, TaxTierBracket.determineTier(100_000_000_000L));
        assertEquals(0.08, TaxTierBracket.OVER_50B.getVatRate());
        assertEquals(0.20, TaxTierBracket.OVER_50B.getPitRate());
        assertEquals(0.28, TaxTierBracket.OVER_50B.getTotalDirectRate());
    }

    @Test
    @DisplayName("Tính thuế cho doanh thu 2 tỷ VNĐ (Khung 2: GTGT 1%, TNCN 0.5%, Tổng 1.5%)")
    void testCalculateTaxSummaryTier2() {
        long revenue = 2_000_000_000L;
        long invoiceCount = 150L;

        when(taxRepository.queryRevenueAndCountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(new Object[]{revenue, invoiceCount}));

        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .build();

        TaxSummaryResponse response = taxCalculationService.calculateTaxSummary(request);

        assertNotNull(response);
        assertEquals(2026, response.getTaxYear());
        assertEquals(2_000_000_000L, response.getAnnualNetRevenue());
        assertEquals(TaxTierBracket.FROM_1B_TO_3B, response.getAppliedTier());
        assertEquals(0.01, response.getVatRate());
        assertEquals(20_000_000L, response.getVatAmount()); // 2B * 1% = 20M
        assertEquals(0.005, response.getPitRate());
        assertEquals(10_000_000L, response.getPitAmount()); // 2B * 0.5% = 10M
        assertEquals(0.015, response.getTotalTaxRate());
        assertEquals(30_000_000L, response.getTotalTaxAmount()); // 20M + 10M = 30M
        assertEquals(150L, response.getTotalPaidInvoices());
    }

    @Test
    @DisplayName("Tính thuế cho doanh thu 10 tỷ VNĐ (Khung 3: GTGT 8%, TNCN 17%, Tổng 25%)")
    void testCalculateTaxSummaryTier3() {
        long revenue = 10_000_000_000L;
        long invoiceCount = 800L;

        when(taxRepository.queryRevenueAndCountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(new Object[]{revenue, invoiceCount}));

        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .build();

        TaxSummaryResponse response = taxCalculationService.calculateTaxSummary(request);

        assertNotNull(response);
        assertEquals(10_000_000_000L, response.getAnnualNetRevenue());
        assertEquals(TaxTierBracket.FROM_3B_TO_50B, response.getAppliedTier());
        assertEquals(0.08, response.getVatRate());
        assertEquals(800_000_000L, response.getVatAmount()); // 10B * 8% = 800M
        assertEquals(0.17, response.getPitRate());
        assertEquals(1_700_000_000L, response.getPitAmount()); // 10B * 17% = 1.7B
        assertEquals(0.25, response.getTotalTaxRate());
        assertEquals(2_500_000_000L, response.getTotalTaxAmount()); // 2.5B
    }

    @Test
    @DisplayName("Doanh thu dưới 1 tỷ được miễn thuế (Khung 1: 0%)")
    void testCalculateTaxSummaryExempt() {
        long revenue = 600_000_000L;
        long invoiceCount = 45L;

        when(taxRepository.queryRevenueAndCountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(new Object[]{revenue, invoiceCount}));

        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .build();

        TaxSummaryResponse response = taxCalculationService.calculateTaxSummary(request);

        assertNotNull(response);
        assertEquals(TaxTierBracket.UNDER_1B, response.getAppliedTier());
        assertEquals(0L, response.getVatAmount());
        assertEquals(0L, response.getPitAmount());
        assertEquals(0L, response.getTotalTaxAmount());
    }

    @Test
    @DisplayName("Edge Case: Doanh thu = 0 VNĐ (Không phát sinh đơn) -> Thuộc Bậc 1, Thuế = 0")
    void testCalculateTaxSummaryZeroRevenue() {
        when(taxRepository.queryRevenueAndCountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .build();

        TaxSummaryResponse response = taxCalculationService.calculateTaxSummary(request);

        assertNotNull(response);
        assertEquals(0L, response.getAnnualNetRevenue());
        assertEquals(TaxTierBracket.UNDER_1B, response.getAppliedTier());
        assertEquals(0L, response.getVatAmount());
        assertEquals(0L, response.getPitAmount());
        assertEquals(0L, response.getTotalTaxAmount());
        assertEquals(0L, response.getTotalPaidInvoices());
    }

    @Test
    @DisplayName("Edge Case: Doanh thu Bậc 4 trên 50 tỷ (>50B: GTGT 8%, TNCN 20%, Tổng 28%)")
    void testCalculateTaxSummaryTier4() {
        long revenue = 100_000_000_000L; // 100 tỷ
        long invoiceCount = 5_000L;

        when(taxRepository.queryRevenueAndCountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(new Object[]{revenue, invoiceCount}));

        TaxReportFilterRequest request = TaxReportFilterRequest.builder()
                .year(2026)
                .build();

        TaxSummaryResponse response = taxCalculationService.calculateTaxSummary(request);

        assertNotNull(response);
        assertEquals(100_000_000_000L, response.getAnnualNetRevenue());
        assertEquals(TaxTierBracket.OVER_50B, response.getAppliedTier());
        assertEquals(0.08, response.getVatRate());
        assertEquals(8_000_000_000L, response.getVatAmount()); // 100B * 8% = 8B
        assertEquals(0.20, response.getPitRate());
        assertEquals(20_000_000_000L, response.getPitAmount()); // 100B * 20% = 20B
        assertEquals(0.28, response.getTotalTaxRate());
        assertEquals(28_000_000_000L, response.getTotalTaxAmount()); // 28B
    }

    @Test
    @DisplayName("Edge Case: Doanh thu chạm đúng mốc 1 tỷ (1,000,000,000 VNĐ) -> Nhảy sang Bậc 2")
    void testTaxTierBoundaryExactly1B() {
        assertEquals(TaxTierBracket.UNDER_1B, TaxTierBracket.determineTier(999_999_999L));
        assertEquals(TaxTierBracket.FROM_1B_TO_3B, TaxTierBracket.determineTier(1_000_000_000L));
    }

    @Test
    @DisplayName("Edge Case: Doanh thu chạm đúng mốc 3 tỷ và 3 tỷ + 1 đồng -> Nhảy từ Bậc 2 sang Bậc 3")
    void testTaxTierBoundaryExactly3B() {
        assertEquals(TaxTierBracket.FROM_1B_TO_3B, TaxTierBracket.determineTier(3_000_000_000L));
        assertEquals(TaxTierBracket.FROM_3B_TO_50B, TaxTierBracket.determineTier(3_000_000_001L));
    }

    @Test
    @DisplayName("Edge Case: Doanh thu chạm đúng mốc 50 tỷ và 50 tỷ + 1 đồng -> Nhảy từ Bậc 3 sang Bậc 4")
    void testTaxTierBoundaryExactly50B() {
        assertEquals(TaxTierBracket.FROM_3B_TO_50B, TaxTierBracket.determineTier(50_000_000_000L));
        assertEquals(TaxTierBracket.OVER_50B, TaxTierBracket.determineTier(50_000_000_001L));
    }
}
