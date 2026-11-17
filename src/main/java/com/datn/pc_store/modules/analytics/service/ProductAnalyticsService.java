package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.DateRangeFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.*;
import com.datn.pc_store.modules.analytics.repository.AnalyticsProductRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
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
public class ProductAnalyticsService {

    private final AnalyticsProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<TopProductDto> getTopSellingProducts(DateRangeFilterRequest filter, int limit) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = productRepository.queryTopSellingProducts(range[0], range[1], limit > 0 ? limit : 10);
        return mapToTopProductDtoList(rows);
    }

    @Transactional(readOnly = true)
    public List<TopProductDto> getTopGrossProfitProducts(DateRangeFilterRequest filter, int limit) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = productRepository.queryTopGrossProfitProducts(range[0], range[1], limit > 0 ? limit : 10);
        return mapToTopProductDtoList(rows);
    }

    @Transactional(readOnly = true)
    public List<ProductMatrixDto> getProductPerformanceMatrix(DateRangeFilterRequest filter) {
        Instant[] range = AnalyticsDateHelper.resolveRange(
                filter != null ? filter.getStartDate() : null,
                filter != null ? filter.getEndDate() : null,
                30
        );

        List<Object[]> rows = productRepository.queryProductPerformanceMatrix(range[0], range[1]);
        List<ProductMatrixDto> result = new ArrayList<>();

        for (Object[] r : rows) {
            Long pid = r[0] != null ? ((Number) r[0]).longValue() : null;
            String pcode = (String) r[1];
            String pname = (String) r[2];
            String cname = (String) r[3];
            String bname = (String) r[4];
            long qtySold = r[5] != null ? ((Number) r[5]).longValue() : 0L;
            long revenue = r[6] != null ? ((Number) r[6]).longValue() : 0L;
            long cogs = r[7] != null ? ((Number) r[7]).longValue() : 0L;
            long profit = r[8] != null ? ((Number) r[8]).longValue() : 0L;
            long currentStock = r[9] != null ? ((Number) r[9]).longValue() : 0L;
            long warrantyCount = r[10] != null ? ((Number) r[10]).longValue() : 0L;

            double margin = revenue > 0
                    ? BigDecimal.valueOf((double) profit * 100.0 / revenue)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(ProductMatrixDto.builder()
                    .productId(pid)
                    .productCode(pcode)
                    .productName(pname)
                    .categoryName(cname)
                    .brandName(bname)
                    .quantitySold(qtySold)
                    .revenue(revenue)
                    .cogs(cogs)
                    .grossProfit(profit)
                    .grossMarginPercentage(margin)
                    .currentStock(currentStock)
                    .warrantyCount(warrantyCount)
                    .build());
        }

        return result;
    }

    private List<TopProductDto> mapToTopProductDtoList(List<Object[]> rows) {
        List<TopProductDto> list = new ArrayList<>();
        for (Object[] r : rows) {
            Long pid = r[0] != null ? ((Number) r[0]).longValue() : null;
            String pcode = (String) r[1];
            String pname = (String) r[2];
            String cname = (String) r[3];
            long qtySold = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            long revenue = r[5] != null ? ((Number) r[5]).longValue() : 0L;
            long cogs = r[6] != null ? ((Number) r[6]).longValue() : 0L;
            long profit = r[7] != null ? ((Number) r[7]).longValue() : 0L;

            double margin = revenue > 0
                    ? BigDecimal.valueOf((double) profit * 100.0 / revenue)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            list.add(TopProductDto.builder()
                    .productId(pid)
                    .productCode(pcode)
                    .productName(pname)
                    .categoryName(cname)
                    .quantitySold(qtySold)
                    .revenue(revenue)
                    .cogs(cogs)
                    .grossProfit(profit)
                    .grossMarginPercentage(margin)
                    .build());
        }
        return list;
    }
}
