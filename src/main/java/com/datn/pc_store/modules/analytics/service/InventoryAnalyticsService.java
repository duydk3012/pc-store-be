package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.InventoryFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.enums.InventoryAgingBracket;
import com.datn.pc_store.modules.analytics.repository.AnalyticsInventoryRepository;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAnalyticsService {

    private final AnalyticsInventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventorySummaryDto getInventorySummary(InventoryFilterRequest filter) {
        List<Object[]> rows = inventoryRepository.queryInventoryOverviewSummary();
        if (rows.isEmpty() || rows.get(0) == null) {
            return InventorySummaryDto.builder().build();
        }

        Object[] r = rows.get(0);
        long totalSerials = r[0] != null ? ((Number) r[0]).longValue() : 0L;
        long totalVal = r[1] != null ? ((Number) r[1]).longValue() : 0L;
        long sellableCount = r[2] != null ? ((Number) r[2]).longValue() : 0L;
        long sellableVal = r[3] != null ? ((Number) r[3]).longValue() : 0L;
        long defectiveCount = r[4] != null ? ((Number) r[4]).longValue() : 0L;
        long committedCount = r[5] != null ? ((Number) r[5]).longValue() : 0L;
        long technicalCount = r[6] != null ? ((Number) r[6]).longValue() : 0L;

        return InventorySummaryDto.builder()
                .totalSerialsInStore(totalSerials)
                .totalInventoryValue(totalVal)
                .sellableSerials(sellableCount)
                .sellableValue(sellableVal)
                .defectiveSerials(defectiveCount)
                .packingAndDeliveringSerials(committedCount)
                .technicalUsageSerials(technicalCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownDto> getStockByCategory(InventoryFilterRequest filter) {
        List<Object[]> rows = inventoryRepository.queryStockByCategory();
        long totalStock = 0L;
        for (Object[] r : rows) {
            totalStock += r[2] != null ? ((Number) r[2]).longValue() : 0L;
        }

        List<CategoryBreakdownDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long catId = r[0] != null ? ((Number) r[0]).longValue() : null;
            String catName = (String) r[1];
            long count = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            double pct = totalStock > 0
                    ? BigDecimal.valueOf((double) count * 100.0 / totalStock)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(CategoryBreakdownDto.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .value(count)
                    .percentage(pct)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<OrderStatusDistributionDto> getSerialStatusDistribution() {
        List<Object[]> rows = inventoryRepository.querySerialStatusCounts();
        long total = 0L;
        for (Object[] r : rows) {
            total += r[1] != null ? ((Number) r[1]).longValue() : 0L;
        }

        List<OrderStatusDistributionDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            int statusVal = r[0] != null ? ((Number) r[0]).intValue() : 0;
            long count = r[1] != null ? ((Number) r[1]).longValue() : 0L;

            String statusName;
            String label;
            try {
                ProductSerialStatus pss = ProductSerialStatus.fromValue(statusVal);
                statusName = pss.name();
                label = switch (pss) {
                    case GOOD_CONDITION -> "Tốt (Có thể bán)";
                    case DEFECTIVE -> "Lỗi kỹ thuật";
                    case IN_STOCK -> "Trong kho (Có thể bán)";
                    case PENDING_RETURN -> "Chờ trả NCC";
                    case RETURNING -> "Đang trả NCC";
                    case RETURNED -> "Đã trả NCC";
                    case PACKING -> "Đang đóng gói";
                    case DELIVERING -> "Đang giao khách";
                    case SOLD -> "Đã bán xuất kho";
                    case USING_BY_TECHNICAL -> "Kỹ thuật sử dụng";
                };
            } catch (Exception e) {
                statusName = "UNKNOWN_" + statusVal;
                label = "Khác (" + statusVal + ")";
            }

            double pct = total > 0
                    ? BigDecimal.valueOf((double) count * 100.0 / total)
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

    @Transactional(readOnly = true)
    public List<InventoryAgingBracketDto> getInventoryAging() {
        List<Object[]> rows = inventoryRepository.queryInventoryAging();
        long totalQty = 0L;
        for (Object[] r : rows) {
            totalQty += r[1] != null ? ((Number) r[1]).longValue() : 0L;
        }

        List<InventoryAgingBracketDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            String bracketCode = (String) r[0];
            long qty = r[1] != null ? ((Number) r[1]).longValue() : 0L;
            long val = r[2] != null ? ((Number) r[2]).longValue() : 0L;

            String label = bracketCode;
            try {
                InventoryAgingBracket b = InventoryAgingBracket.valueOf(bracketCode);
                label = b.getLabel();
            } catch (Exception ignored) {}

            double pct = totalQty > 0
                    ? BigDecimal.valueOf((double) qty * 100.0 / totalQty)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(InventoryAgingBracketDto.builder()
                    .bracket(bracketCode)
                    .label(label)
                    .quantity(qty)
                    .totalValue(val)
                    .percentage(pct)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<LowStockProductDto> getLowStockAlerts(InventoryFilterRequest filter) {
        int threshold = (filter != null && filter.getMinThreshold() != null && filter.getMinThreshold() >= 0)
                ? filter.getMinThreshold()
                : 5;

        List<Object[]> rows = inventoryRepository.queryLowStockAlerts(threshold);
        List<LowStockProductDto> result = new ArrayList<>();

        for (Object[] r : rows) {
            Long pid = r[0] != null ? ((Number) r[0]).longValue() : null;
            String pcode = (String) r[1];
            String pname = (String) r[2];
            String cname = (String) r[3];
            long sellableQty = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            String sname = r[5] != null ? (String) r[5] : "Chưa có NCC chỉ định";

            result.add(LowStockProductDto.builder()
                    .productId(pid)
                    .productCode(pcode)
                    .productName(pname)
                    .categoryName(cname)
                    .sellableQuantity(sellableQty)
                    .minimumThreshold(threshold)
                    .suggestedSupplier(sname)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<SlowMovingProductDto> getSlowMovingProducts(int thresholdDays) {
        int days = thresholdDays > 0 ? thresholdDays : 30;
        List<Object[]> rows = inventoryRepository.querySlowMovingProducts(days);
        List<SlowMovingProductDto> result = new ArrayList<>();

        for (Object[] r : rows) {
            Long pid = r[0] != null ? ((Number) r[0]).longValue() : null;
            String pcode = (String) r[1];
            String pname = (String) r[2];
            String cname = (String) r[3];
            long sellableQty = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            Instant lastSoldDate = null;
            if (r[5] instanceof Timestamp ts) {
                lastSoldDate = ts.toInstant();
            } else if (r[5] instanceof Instant inst) {
                lastSoldDate = inst;
            }
            long daysWithoutSale = r[6] != null ? ((Number) r[6]).longValue() : 0L;

            result.add(SlowMovingProductDto.builder()
                    .productId(pid)
                    .productCode(pcode)
                    .productName(pname)
                    .categoryName(cname)
                    .sellableQuantity(sellableQty)
                    .lastSoldDate(lastSoldDate)
                    .daysWithoutSale(daysWithoutSale)
                    .build());
        }

        return result;
    }
}
