package com.datn.pc_store.modules.order.repository;

import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrderId(Long orderId);

    boolean existsByProductIdAndOrder_UserIdAndOrder_Status(Long productId, Long userId, OrderStatus status);

    @org.springframework.data.jpa.repository.Query("""
        SELECT oi.productId FROM OrderItem oi
        JOIN oi.order o
        WHERE o.userId = :userId
          AND o.status = com.datn.pc_store.modules.order.enums.OrderStatus.COMPLETED
        GROUP BY oi.productId
        ORDER BY MAX(oi.id) DESC
    """)
    List<Long> findRecentPurchasedProductIds(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);
}

