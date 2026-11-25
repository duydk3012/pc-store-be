package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.modules.analytics.dto.request.TaxReportFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.enums.TaxTierBracket;
import com.datn.pc_store.modules.analytics.repository.AnalyticsTaxRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
import com.datn.pc_store.modules.order.enums.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private final AnalyticsTaxRepository taxRepository;

    @Transactional(readOnly = true)
    public TaxSummaryResponse calculateTaxSummary(TaxReportFilterRequest filter) {
        int year = (filter != null && filter.getYear() != null)
                ? filter.getYear()
                : LocalDate.now(AnalyticsDateHelper.VN_ZONE).getYear();

        Integer quarter = filter != null ? filter.getQuarter() : null;
        Integer month = filter != null ? filter.getMonth() : null;

        if (quarter != null && month != null) {
            throw new BadRequestException("Chỉ chọn lọc theo Quý hoặc theo Tháng, không chọn đồng thời cả hai.");
        }

        // 1. Luôn xác định tổng doanh thu cả năm để tìm bậc thuế suất luỹ tiến áp dụng
        Instant[] yearRange = AnalyticsDateHelper.resolveYearRange(year);
        List<Object[]> annualStats = taxRepository.queryRevenueAndCountBetween(yearRange[0], yearRange[1]);
        long annualRevenue = 0L;
        long annualInvoices = 0L;
        if (!annualStats.isEmpty() && annualStats.get(0) != null) {
            annualRevenue = ((Number) annualStats.get(0)[0]).longValue();
            annualInvoices = ((Number) annualStats.get(0)[1]).longValue();
        }

        TaxTierBracket tier = TaxTierBracket.determineTier(annualRevenue);

        // 2. Doanh thu tính thuế của kỳ được chọn (cả năm, theo quý hoặc theo tháng)
        long periodRevenue = annualRevenue;
        long periodInvoices = annualInvoices;
        if (quarter != null || month != null) {
            Instant[] periodRange = quarter != null
                    ? AnalyticsDateHelper.resolveQuarterRange(year, quarter)
                    : AnalyticsDateHelper.resolveMonthRange(year, month);
            List<Object[]> periodStats = taxRepository.queryRevenueAndCountBetween(periodRange[0], periodRange[1]);
            if (!periodStats.isEmpty() && periodStats.get(0) != null) {
                periodRevenue = ((Number) periodStats.get(0)[0]).longValue();
                periodInvoices = ((Number) periodStats.get(0)[1]).longValue();
            } else {
                periodRevenue = 0L;
                periodInvoices = 0L;
            }
        }

        long vatAmount = Math.round(periodRevenue * tier.getVatRate());
        long pitAmount = Math.round(periodRevenue * tier.getPitRate());
        long totalTaxAmount = Math.round(periodRevenue * tier.getTotalDirectRate());

        return TaxSummaryResponse.builder()
                .taxYear(year)
                .quarter(quarter)
                .annualNetRevenue(periodRevenue)
                .appliedTier(tier)
                .tierDescription(tier.getDescription())
                .vatRate(tier.getVatRate())
                .vatAmount(vatAmount)
                .pitRate(tier.getPitRate())
                .pitAmount(pitAmount)
                .totalTaxRate(tier.getTotalDirectRate())
                .totalTaxAmount(totalTaxAmount)
                .totalPaidInvoices(periodInvoices)
                .generatedAt(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public TaxQuarterlyResponse getTaxQuarterlyBreakdown(int year) {
        Instant[] yearRange = AnalyticsDateHelper.resolveYearRange(year);
        List<Object[]> annualStats = taxRepository.queryRevenueAndCountBetween(yearRange[0], yearRange[1]);
        long annualRevenue = 0L;
        if (!annualStats.isEmpty() && annualStats.get(0) != null) {
            annualRevenue = ((Number) annualStats.get(0)[0]).longValue();
        }

        TaxTierBracket tier = TaxTierBracket.determineTier(annualRevenue);

        List<Object[]> rows = taxRepository.queryQuarterlyRevenueBetween(yearRange[0], yearRange[1]);
        Map<Integer, long[]> qData = new HashMap<>();
        for (Object[] r : rows) {
            int q = r[0] != null ? ((Number) r[0]).intValue() : 0;
            long rev = r[1] != null ? ((Number) r[1]).longValue() : 0L;
            long cnt = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            qData.put(q, new long[]{rev, cnt});
        }

        List<TaxQuarterDto> quarters = new ArrayList<>();
        for (int q = 1; q <= 4; q++) {
            long qRev = qData.containsKey(q) ? qData.get(q)[0] : 0L;
            long qCnt = qData.containsKey(q) ? qData.get(q)[1] : 0L;

            long vat = Math.round(qRev * tier.getVatRate());
            long pit = Math.round(qRev * tier.getPitRate());
            long total = Math.round(qRev * tier.getTotalDirectRate());

            quarters.add(TaxQuarterDto.builder()
                    .quarter(q)
                    .quarterRevenue(qRev)
                    .vatAmount(vat)
                    .pitAmount(pit)
                    .totalTax(total)
                    .invoiceCount(qCnt)
                    .build());
        }

        return TaxQuarterlyResponse.builder()
                .taxYear(year)
                .annualNetRevenue(annualRevenue)
                .appliedTaxRate(tier.getTotalDirectRate())
                .quarters(quarters)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaxInvoiceDetailDto> getTaxInvoices(TaxReportFilterRequest filter, Pageable pageable) {
        int year = (filter != null && filter.getYear() != null)
                ? filter.getYear()
                : LocalDate.now(AnalyticsDateHelper.VN_ZONE).getYear();

        Integer quarter = filter != null ? filter.getQuarter() : null;
        Integer month = filter != null ? filter.getMonth() : null;
        if (quarter != null && month != null) {
            throw new BadRequestException("Chỉ chọn lọc theo Quý hoặc theo Tháng, không chọn đồng thời cả hai.");
        }

        Instant[] range;
        if (quarter != null) {
            range = AnalyticsDateHelper.resolveQuarterRange(year, quarter);
        } else if (month != null) {
            range = AnalyticsDateHelper.resolveMonthRange(year, month);
        } else {
            range = AnalyticsDateHelper.resolveYearRange(year);
        }

        // Xác định bậc thuế suất áp dụng
        Instant[] yearRange = AnalyticsDateHelper.resolveYearRange(year);
        List<Object[]> annualStats = taxRepository.queryRevenueAndCountBetween(yearRange[0], yearRange[1]);
        long annualRevenue = (!annualStats.isEmpty() && annualStats.get(0) != null)
                ? ((Number) annualStats.get(0)[0]).longValue() : 0L;
        TaxTierBracket tier = TaxTierBracket.determineTier(annualRevenue);

        long totalElements = taxRepository.countTaxInvoicesBetween(range[0], range[1]);
        Pageable nativePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Object[]> rows = taxRepository.queryTaxInvoicesPaged(range[0], range[1], nativePageable);

        List<TaxInvoiceDetailDto> dtos = new ArrayList<>();
        for (Object[] r : rows) {
            Long invId = r[0] != null ? ((Number) r[0]).longValue() : null;
            String invCode = (String) r[1];
            Instant invDate = null;
            if (r[2] instanceof Timestamp ts) invDate = ts.toInstant();
            else if (r[2] instanceof Instant inst) invDate = inst;

            String orderCode = (String) r[3];
            String custName = (String) r[4];
            String custPhone = (String) r[5];
            long totalAmt = r[6] != null ? ((Number) r[6]).longValue() : 0L;
            long discAmt = r[7] != null ? ((Number) r[7]).longValue() : 0L;
            long amount = r[8] != null ? ((Number) r[8]).longValue() : 0L;
            int statusVal = r[9] != null ? ((Number) r[9]).intValue() : 2;

            long vat = Math.round(amount * tier.getVatRate());
            long pit = Math.round(amount * tier.getPitRate());

            dtos.add(TaxInvoiceDetailDto.builder()
                    .invoiceId(invId)
                    .invoiceCode(invCode)
                    .invoiceDate(invDate)
                    .orderCode(orderCode)
                    .customerName(custName)
                    .customerPhone(custPhone)
                    .totalAmount(totalAmt)
                    .discountAmount(discAmt)
                    .amount(amount)
                    .vatAmount(vat)
                    .pitAmount(pit)
                    .status(InvoiceStatus.fromValue(statusVal))
                    .build());
        }

        return PageResponse.of(new PageImpl<>(dtos, pageable, totalElements));
    }
}
