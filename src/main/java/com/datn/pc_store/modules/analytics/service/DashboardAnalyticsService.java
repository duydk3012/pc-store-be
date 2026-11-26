package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.repository.AnalyticsDashboardRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private final AnalyticsDashboardRepository dashboardRepository;

    @Transactional(readOnly = true)
    public DashboardKpiResponse getDashboardKpis(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );
        Instant fromTime = range[0];
        Instant toTime = range[1];

        long totalRevenue = dashboardRepository.sumRevenueBetween(fromTime, toTime);
        long totalPaidInvoices = dashboardRepository.countPaidInvoicesBetween(fromTime, toTime);
        long totalProductsSold = dashboardRepository.sumProductsSoldBetween(fromTime, toTime);
        long cogs = dashboardRepository.sumCogsBetween(fromTime, toTime);
        long grossProfit = totalRevenue - cogs;

        double grossMargin = 0.0;
        if (totalRevenue > 0) {
            grossMargin = BigDecimal.valueOf((double) grossProfit * 100.0 / totalRevenue)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long inventoryValue = dashboardRepository.sumCurrentSellableInventoryValue();
        long lowStockCount = dashboardRepository.countLowStockProducts(5);
        long processingOrders = dashboardRepository.countProcessingOrders();

        return DashboardKpiResponse.builder()
                .totalRevenue(totalRevenue)
                .totalPaidInvoices(totalPaidInvoices)
                .totalProductsSold(totalProductsSold)
                .grossProfit(grossProfit)
                .grossMarginPercentage(grossMargin)
                .totalInventoryValue(inventoryValue)
                .lowStockProductCount(lowStockCount)
                .processingOrderCount(processingOrders)
                .asOfDate(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesPointDto> getRevenueAndOrderTrend(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = dashboardRepository.queryRevenueAndOrderTrendDaily(range[0], range[1]);
        List<TimeSeriesPointDto> points = new ArrayList<>();

        for (Object[] r : rows) {
            String timeBucket = (String) r[0];
            String label = (String) r[1];
            long revenue = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            long orderCount = r[3] != null ? ((Number) r[3]).longValue() : 0L;

            points.add(TimeSeriesPointDto.builder()
                    .timeBucket(timeBucket)
                    .label(label)
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .build());
        }

        return points;
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownDto> getRevenueByCategory(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = dashboardRepository.queryRevenueByCategoryHierarchy(range[0], range[1]);
        return buildHierarchicalCategoryBreakdown(rows, null);
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownDto> getRevenueByCategoryMarketing(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = dashboardRepository.queryRevenueByAllLinkedCategories(range[0], range[1]);
        long totalStoreRevenue = dashboardRepository.sumRevenueBetween(range[0], range[1]);
        return buildHierarchicalCategoryBreakdown(rows, totalStoreRevenue);
    }

    private List<CategoryBreakdownDto> buildHierarchicalCategoryBreakdown(List<Object[]> rows, Long overrideTotalDenominator) {
        java.util.Map<Long, String> rootNames = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Long> rootTotals = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Long> rootDirectOthers = new java.util.HashMap<>();
        java.util.Map<Long, List<CategoryBreakdownDto>> rootChildrenMap = new java.util.HashMap<>();

        for (Object[] r : rows) {
            Long rootId = r[0] != null ? ((Number) r[0]).longValue() : 0L;
            String rootName = r[1] != null ? (String) r[1] : "Chưa phân loại";
            Long childId = r[2] != null ? ((Number) r[2]).longValue() : null;
            String childName = r[3] != null ? (String) r[3] : null;
            long rev = r[4] != null ? ((Number) r[4]).longValue() : 0L;

            rootNames.putIfAbsent(rootId, rootName);
            rootTotals.merge(rootId, rev, Long::sum);

            if (childId != null && childName != null) {
                rootChildrenMap.computeIfAbsent(rootId, k -> new ArrayList<>())
                        .add(CategoryBreakdownDto.builder()
                                .categoryId(childId)
                                .categoryName(childName)
                                .value(rev)
                                .percentage(0.0)
                                .children(List.of())
                                .build());
            } else {
                rootDirectOthers.merge(rootId, rev, Long::sum);
            }
        }

        List<Long> sortedRootIds = rootTotals.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(java.util.Map.Entry::getKey)
                .toList();

        long denominator = (overrideTotalDenominator != null && overrideTotalDenominator > 0)
                ? overrideTotalDenominator
                : sortedRootIds.stream().mapToLong(id -> rootTotals.getOrDefault(id, 0L)).sum();

        List<CategoryBreakdownDto> result = new ArrayList<>();
        for (Long rootId : sortedRootIds) {
            long rootRev = rootTotals.getOrDefault(rootId, 0L);
            double rootPct = calculatePercentage(rootRev, denominator);

            List<CategoryBreakdownDto> childDtos = new ArrayList<>();
            List<CategoryBreakdownDto> rawChildren = rootChildrenMap.getOrDefault(rootId, List.of());

            if (!rawChildren.isEmpty()) {
                for (CategoryBreakdownDto child : rawChildren) {
                    child.setPercentage(calculatePercentage(child.getValue(), rootRev));
                    childDtos.add(child);
                }

                long directOtherRev = rootDirectOthers.getOrDefault(rootId, 0L);
                if (directOtherRev > 0) {
                    childDtos.add(CategoryBreakdownDto.builder()
                            .categoryId(0L)
                            .categoryName("Khác")
                            .value(directOtherRev)
                            .percentage(calculatePercentage(directOtherRev, rootRev))
                            .children(List.of())
                            .build());
                }

                childDtos.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            }

            result.add(CategoryBreakdownDto.builder()
                    .categoryId(rootId)
                    .categoryName(rootNames.get(rootId))
                    .value(rootRev)
                    .percentage(rootPct)
                    .children(childDtos)
                    .build());
        }

        return result;
    }

    private double calculatePercentage(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) part * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Transactional(readOnly = true)
    public List<OrderStatusDistributionDto> getOrderStatusDistribution() {
        List<Object[]> rows = dashboardRepository.queryOrderStatusCounts();
        long totalOrders = 0L;
        for (Object[] r : rows) {
            totalOrders += r[1] != null ? ((Number) r[1]).longValue() : 0L;
        }

        List<OrderStatusDistributionDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            int statusVal = r[0] != null ? ((Number) r[0]).intValue() : 0;
            long count = r[1] != null ? ((Number) r[1]).longValue() : 0L;

            String statusName;
            String label;
            try {
                OrderStatus os = OrderStatus.fromValue(statusVal);
                statusName = os.name();
                label = switch (os) {
                    case PENDING -> "Chờ xác nhận";
                    case CONFIRMED -> "Đã xác nhận";
                    case REJECTED -> "Từ chối";
                    case DELIVERING -> "Đang giao hàng";
                    case COMPLETED -> "Hoàn tất";
                    case CANCELLED -> "Đã hủy";
                    case RETURNED -> "Đã trả hàng";
                };
            } catch (Exception e) {
                statusName = "UNKNOWN_" + statusVal;
                label = "Khác (" + statusVal + ")";
            }

            double pct = totalOrders > 0
                    ? BigDecimal.valueOf((double) count * 100.0 / totalOrders)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(OrderStatusDistributionDto.builder()
                    .status(statusName)
                    .label(label)
                    .count(count)
                    .percentage(pct)
                    .build());
        }

        return result;
    }
}
