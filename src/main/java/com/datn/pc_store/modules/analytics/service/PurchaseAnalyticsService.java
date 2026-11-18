package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.PurchaseFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.enums.GroupByTime;
import com.datn.pc_store.modules.analytics.repository.AnalyticsPurchaseRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
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
public class PurchaseAnalyticsService {

    private final AnalyticsPurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public List<TimeSeriesPointDto> getPurchasingTrend(PurchaseFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        GroupByTime groupBy = (filter != null && filter.getGroupBy() != null) ? filter.getGroupBy() : GroupByTime.AUTO;
        List<Object[]> rows;
        if (groupBy == GroupByTime.MONTH) {
            rows = purchaseRepository.queryPurchasingTrendMonthly(range[0], range[1]);
        } else {
            rows = purchaseRepository.queryPurchasingTrendDaily(range[0], range[1]);
        }

        List<TimeSeriesPointDto> points = new ArrayList<>();
        for (Object[] r : rows) {
            String timeBucket = (String) r[0];
            String label = (String) r[1];
            long importVal = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            long count = r[3] != null ? ((Number) r[3]).longValue() : 0L;

            points.add(TimeSeriesPointDto.builder()
                    .timeBucket(timeBucket)
                    .label(label)
                    .revenue(importVal)
                    .orderCount(count)
                    .build());
        }

        return points;
    }

    @Transactional(readOnly = true)
    public List<SupplierPerformanceDto> getPurchasingBySupplier(PurchaseFilterRequest filter, int limit) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = purchaseRepository.queryPurchasingBySupplierTopN(
                range[0], range[1], limit > 0 ? limit : 10
        );

        List<SupplierPerformanceDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long sid = r[0] != null ? ((Number) r[0]).longValue() : null;
            String sname = (String) r[1];
            String scode = (String) r[2];
            long importVal = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            long rcount = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            long qtyReceived = r[5] != null ? ((Number) r[5]).longValue() : 0L;
            long qtyAccepted = r[6] != null ? ((Number) r[6]).longValue() : 0L;
            long qtyDefective = r[7] != null ? ((Number) r[7]).longValue() : 0L;

            double defectRate = qtyReceived > 0
                    ? BigDecimal.valueOf((double) qtyDefective * 100.0 / qtyReceived)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(SupplierPerformanceDto.builder()
                    .supplierId(sid)
                    .supplierName(sname)
                    .supplierCode(scode)
                    .totalImportValue(importVal)
                    .receiptCount(rcount)
                    .totalQuantityReceived(qtyReceived)
                    .acceptedQuantity(qtyAccepted)
                    .defectiveQuantity(qtyDefective)
                    .defectRatePercentage(defectRate)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ReceiptQualityDto> getQualityInspection(PurchaseFilterRequest filter) {
        List<SupplierPerformanceDto> suppliers = getPurchasingBySupplier(filter, 20);
        List<ReceiptQualityDto> result = new ArrayList<>();

        for (SupplierPerformanceDto s : suppliers) {
            result.add(ReceiptQualityDto.builder()
                    .label(s.getSupplierName())
                    .acceptedQuantity(s.getAcceptedQuantity())
                    .defectiveQuantity(s.getDefectiveQuantity())
                    .totalQuantity(s.getTotalQuantityReceived())
                    .defectRatePercentage(s.getDefectRatePercentage())
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<OrderStatusDistributionDto> getReceiptStatusDistribution(PurchaseFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = purchaseRepository.queryReceiptStatusCounts(range[0], range[1]);
        long totalReceipts = 0L;
        for (Object[] r : rows) {
            totalReceipts += r[1] != null ? ((Number) r[1]).longValue() : 0L;
        }

        List<OrderStatusDistributionDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            int statusVal = r[0] != null ? ((Number) r[0]).intValue() : 0;
            long count = r[1] != null ? ((Number) r[1]).longValue() : 0L;

            String statusName;
            String label;
            try {
                GoodsReceiptStatus grs = GoodsReceiptStatus.fromValue(statusVal);
                statusName = grs.name();
                label = switch (grs) {
                    case PENDING -> "Chờ xử lý";
                    case CONFIRMED -> "Đã xác nhận";
                    case REJECTED -> "Từ chối";
                    case CANCELLED -> "Đã hủy";
                    case DRAFT -> "Nháp";
                };
            } catch (Exception e) {
                statusName = "UNKNOWN_" + statusVal;
                label = "Khác (" + statusVal + ")";
            }

            double pct = totalReceipts > 0
                    ? BigDecimal.valueOf((double) count * 100.0 / totalReceipts)
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
