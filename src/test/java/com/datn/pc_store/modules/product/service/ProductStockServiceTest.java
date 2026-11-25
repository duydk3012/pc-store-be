package com.datn.pc_store.modules.product.service;

import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {

    @Mock private ProductSerialRepository productSerialRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ProductStockService productStockService;

    @Test
    @DisplayName("availableStock = max(0, physicalInStock - reservedUnassignedQty)")
    void getAvailableStockMap_subtractsUnassignedReservedOrders() {
        Product p1 = Product.builder().id(10001L).status(ProductStatus.ACTIVE).isDeleted(false).build();
        Product p2 = Product.builder().id(10002L).status(ProductStatus.OUT_OF_STOCK).isDeleted(false).build();

        when(productSerialRepository.countActiveSerialsByProductIdsAndStatus(List.of(10001L), ProductSerialStatus.IN_STOCK))
                .thenReturn(List.<Object[]>of(new Object[]{10001L, 5L}));
        when(orderItemRepository.sumUnassignedOrderedQuantitiesByProductIds(List.of(10001L)))
                .thenReturn(List.<Object[]>of(new Object[]{10001L, 3L}));

        Map<Long, Integer> stockMap = productStockService.getAvailableStockMap(List.of(p1, p2));

        assertEquals(2, stockMap.get(10001L));
        assertEquals(0, stockMap.get(10002L));
    }

    @Test
    @DisplayName("syncProductStockStatus đồng bộ ACTIVE <-> OUT_OF_STOCK và không đụng vào INACTIVE")
    void syncProductStockStatus_updatesActiveAndOutOfStockOnly() {
        Product activeZero = Product.builder().id(1L).status(ProductStatus.ACTIVE).isDeleted(false).build();
        Product outOfStockRestocked = Product.builder().id(2L).status(ProductStatus.OUT_OF_STOCK).isDeleted(false).build();
        Product inactiveProduct = Product.builder().id(3L).status(ProductStatus.INACTIVE).isDeleted(false).build();

        when(productRepository.findAllByIdForUpdate(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(activeZero, outOfStockRestocked, inactiveProduct));
        when(productSerialRepository.countActiveSerialsByProductIdsAndStatus(List.of(1L, 2L, 3L), ProductSerialStatus.IN_STOCK))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, 2L},
                        new Object[]{2L, 4L},
                        new Object[]{3L, 5L}
                ));
        when(orderItemRepository.sumUnassignedOrderedQuantitiesByProductIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, 2L},
                        new Object[]{2L, 1L}
                ));

        productStockService.syncProductStockStatus(List.of(3L, 1L, 2L));

        assertEquals(ProductStatus.OUT_OF_STOCK, activeZero.getStatus());
        assertEquals(ProductStatus.ACTIVE, outOfStockRestocked.getStatus());
        assertEquals(ProductStatus.INACTIVE, inactiveProduct.getStatus());
        verify(productRepository).saveAllAndFlush(anyList());
    }
}
