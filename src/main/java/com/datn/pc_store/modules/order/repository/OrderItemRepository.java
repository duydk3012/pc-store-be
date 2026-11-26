package com.datn.pc_store.modules.order.repository;

import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrderId(Long orderId);

    boolean existsByProductIdAndOrder_UserIdAndOrder_Status(Long productId, Long userId, OrderStatus status);

    @Query("""
        SELECT oi.productId FROM OrderItem oi
        JOIN oi.order o
        WHERE o.userId = :userId
          AND o.status = com.datn.pc_store.modules.order.enums.OrderStatus.COMPLETED
        GROUP BY oi.productId
        ORDER BY MAX(oi.id) DESC
    """)
    List<Long> findRecentPurchasedProductIds(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(value = """
        SELECT oi.product_id,
               COALESCE(SUM(GREATEST(0, oi.quantity - COALESCE(assigned.cnt, 0))), 0) AS unassignedQty
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        LEFT JOIN (
            SELECT order_item_id, COUNT(id) AS cnt
            FROM order_item_serial
            GROUP BY order_item_id
        ) assigned ON assigned.order_item_id = oi.id
        WHERE oi.product_id IN (:productIds)
          AND o.status IN (0, 1)
        GROUP BY oi.product_id
    """, nativeQuery = true)
    List<Object[]> sumUnassignedOrderedQuantitiesByProductIds(@Param("productIds") Collection<Long> productIds);
}

