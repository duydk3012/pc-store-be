package com.datn.pc_store.modules.product.service;

import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dịch vụ tính toán tồn kho khả dụng (availableStock) dựa trên serial IN_STOCK
 * trừ đi số lượng đã đặt trong các đơn hàng đang hoạt động (PENDING/CONFIRMED) chưa gán serial,
 * đồng thời đồng bộ tự động trạng thái ACTIVE <-> OUT_OF_STOCK của sản phẩm.
 */
@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductSerialRepository productSerialRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Map<Long, Integer> computeRawSellableStockMap(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<Long> distinctIds = productIds.stream().distinct().toList();

        Map<Long, Long> inStockMap = new HashMap<>();
        List<Object[]> serialCounts = productSerialRepository.countActiveSerialsByProductIdsAndStatus(
                distinctIds, ProductSerialStatus.IN_STOCK
        );
        if (serialCounts != null) {
            for (Object[] row : serialCounts) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    long pid = ((Number) row[0]).longValue();
                    long count = ((Number) row[1]).longValue();
                    inStockMap.put(pid, count);
                }
            }
        }

        Map<Long, Long> reservedMap = new HashMap<>();
        List<Object[]> reservedCounts = orderItemRepository.sumUnassignedOrderedQuantitiesByProductIds(distinctIds);
        if (reservedCounts != null) {
            for (Object[] row : reservedCounts) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    long pid = ((Number) row[0]).longValue();
                    long qty = ((Number) row[1]).longValue();
                    reservedMap.put(pid, qty);
                }
            }
        }

        Map<Long, Integer> result = new HashMap<>();
        for (Long pid : distinctIds) {
            long inStock = inStockMap.getOrDefault(pid, 0L);
            long reserved = reservedMap.getOrDefault(pid, 0L);
            int available = (int) Math.max(0L, inStock - reserved);
            result.put(pid, available);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> getAvailableStockMap(Collection<Product> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        List<Long> activeIds = products.stream()
                .filter(p -> p != null && !p.isDeleted() && p.getStatus() == ProductStatus.ACTIVE)
                .map(Product::getId)
                .distinct()
                .toList();

        Map<Long, Integer> rawMap = activeIds.isEmpty() ? Map.of() : computeRawSellableStockMap(activeIds);
        Map<Long, Integer> result = new HashMap<>();
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            if (product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
                result.put(product.getId(), 0);
            } else {
                result.put(product.getId(), rawMap.getOrDefault(product.getId(), 0));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public int getAvailableStock(Product product) {
        if (product == null || product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
            return 0;
        }
        return computeRawSellableStockMap(List.of(product.getId())).getOrDefault(product.getId(), 0);
    }

    @Transactional
    public void syncProductStockStatus(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Long> sortedIds = productIds.stream().distinct().sorted().toList();
        List<Product> products = productRepository.findAllByIdForUpdate(sortedIds);
        if (products.isEmpty()) {
            return;
        }
        Map<Long, Integer> rawMap = computeRawSellableStockMap(sortedIds);
        List<Product> changed = new ArrayList<>();
        for (Product product : products) {
            if (product.getStatus() == ProductStatus.INACTIVE) {
                continue;
            }
            int available = rawMap.getOrDefault(product.getId(), 0);
            if (product.getStatus() == ProductStatus.ACTIVE && available == 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
                changed.add(product);
            } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK && available > 0) {
                product.setStatus(ProductStatus.ACTIVE);
                changed.add(product);
            }
        }
        if (!changed.isEmpty()) {
            productRepository.saveAllAndFlush(changed);
        }
    }
}
