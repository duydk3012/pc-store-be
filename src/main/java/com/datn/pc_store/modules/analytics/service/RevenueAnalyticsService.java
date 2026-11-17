package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.enums.ComparePeriod;
import com.datn.pc_store.modules.analytics.enums.GroupByTime;
import com.datn.pc_store.modules.analytics.repository.AnalyticsDashboardRepository;
import com.datn.pc_store.modules.analytics.repository.AnalyticsRevenueRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueAnalyticsService {

    private final AnalyticsRevenueRepository revenueRepository;
    private final AnalyticsDashboardRepository dashboardRepository;

    @Transactional(readOnly = true)
    public List<TimeSeriesPointDto> getRevenueAndProfitTrend(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        GroupByTime groupBy = (filter != null && filter.getGroupBy() != null) ? filter.getGroupBy() : GroupByTime.AUTO;
        List<Object[]> rows;
        if (groupBy == GroupByTime.MONTH) {
            rows = revenueRepository.queryRevenueAndProfitTrendMonthly(range[0], range[1]);
        } else {
            rows = revenueRepository.queryRevenueAndProfitTrendDaily(range[0], range[1]);
        }

        List<TimeSeriesPointDto> points = new ArrayList<>();
        for (Object[] r : rows) {
            String timeBucket = (String) r[0];
            String label = (String) r[1];
            long rev = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            long orders = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            long cogs = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            long profit = rev - cogs;

            points.add(TimeSeriesPointDto.builder()
                    .timeBucket(timeBucket)
                    .label(label)
                    .revenue(rev)
                    .cogs(cogs)
                    .grossProfit(profit)
                    .orderCount(orders)
                    .build());
        }

        return points;
    }

    @Transactional(readOnly = true)
    public PeriodComparisonDto compareRevenueBetweenPeriods(DateRangeFilterRequest filter) {
        LocalDate start = (filter != null && filter.getStartDate() != null)
                ? filter.getStartDate()
                : LocalDate.now(AnalyticsDateHelper.VN_ZONE).minusDays(30);
        LocalDate end = (filter != null && filter.getEndDate() != null)
                ? filter.getEndDate()
                : LocalDate.now(AnalyticsDateHelper.VN_ZONE);

        Instant[] curRange = AnalyticsDateHelper.resolveRange(start, end, 30);
        long curRevenue = dashboardRepository.sumRevenueBetween(curRange[0], curRange[1]);

        ComparePeriod compareWith = (filter != null && filter.getCompareWith() != null)
                ? filter.getCompareWith()
                : ComparePeriod.PREVIOUS_PERIOD;

        LocalDate compStart;
        LocalDate compEnd;
        String compLabel;

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (compareWith == ComparePeriod.SAME_PERIOD_LAST_YEAR) {
            compStart = start.minusYears(1);
            compEnd = end.minusYears(1);
            compLabel = "Cùng kỳ năm trước (" + compStart + " đến " + compEnd + ")";
        } else {
            compEnd = start.minusDays(1);
            compStart = compEnd.minusDays(days - 1);
            compLabel = "Kỳ trước liền kề (" + compStart + " đến " + compEnd + ")";
        }

        Instant compFromTime = compStart.atStartOfDay(AnalyticsDateHelper.VN_ZONE).toInstant();
        Instant compToTime = compEnd.atTime(java.time.LocalTime.MAX).atZone(AnalyticsDateHelper.VN_ZONE).toInstant();
        long compRevenue = dashboardRepository.sumRevenueBetween(compFromTime, compToTime);

        long growthAmount = curRevenue - compRevenue;
        double growthRate = compRevenue > 0
                ? BigDecimal.valueOf((double) growthAmount * 100.0 / compRevenue)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return PeriodComparisonDto.builder()
                .currentRevenue(curRevenue)
                .comparedRevenue(compRevenue)
                .growthAmount(growthAmount)
                .growthRatePercentage(growthRate)
                .currentPeriodLabel(start + " đến " + end)
                .comparedPeriodLabel(compLabel)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BrandBreakdownDto> getRevenueByBrand(DateRangeFilterRequest filter, int limit) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = revenueRepository.queryRevenueByBrandTopN(range[0], range[1], limit > 0 ? limit : 10);
        long totalBrandRevenue = 0L;
        for (Object[] r : rows) {
            totalBrandRevenue += r[2] != null ? ((Number) r[2]).longValue() : 0L;
        }

        List<BrandBreakdownDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long brandId = r[0] != null ? ((Number) r[0]).longValue() : null;
            String brandName = (String) r[1];
            long rev = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            double pct = totalBrandRevenue > 0
                    ? BigDecimal.valueOf((double) rev * 100.0 / totalBrandRevenue)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(BrandBreakdownDto.builder()
                    .brandId(brandId)
                    .brandName(brandName)
                    .revenue(rev)
                    .percentage(pct)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodBreakdownDto> getRevenueByPaymentMethod(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = revenueRepository.queryRevenueByPaymentMethod(range[0], range[1]);
        long totalPayment = 0L;
        for (Object[] r : rows) {
            totalPayment += r[2] != null ? ((Number) r[2]).longValue() : 0L;
        }

        List<PaymentMethodBreakdownDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long methodId = r[0] != null ? ((Number) r[0]).longValue() : null;
            String methodName = (String) r[1];
            long amount = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            double pct = totalPayment > 0
                    ? BigDecimal.valueOf((double) amount * 100.0 / totalPayment)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(PaymentMethodBreakdownDto.builder()
                    .methodId(methodId)
                    .methodName(methodName)
                    .amount(amount)
                    .percentage(pct)
                    .build());
        }

        return result;
    }
}
