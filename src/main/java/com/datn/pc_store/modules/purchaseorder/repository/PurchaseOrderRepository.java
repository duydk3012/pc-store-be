package com.datn.pc_store.modules.purchaseorder.repository;

import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrder;
import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findForUpdate(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM purchase_orders WHERE code = :code AND (:id IS NULL OR id <> :id)", nativeQuery = true)
    long countByCodeIncludingDeleted(@Param("code") String code, @Param("id") Long id);

    @Query("""
        SELECT po FROM PurchaseOrder po
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(po.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(po.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR po.status = :status)
          AND (:supplierId IS NULL OR po.supplierId = :supplierId)
          AND (:fromDate IS NULL OR po.orderDate >= :fromDate)
          AND (:toDate IS NULL OR po.orderDate <= :toDate)
        """)
    Page<PurchaseOrder> searchOrders(@Param("keyword") String keyword,
                                    @Param("status") PurchaseOrderStatus status,
                                    @Param("supplierId") Long supplierId,
                                    @Param("fromDate") Instant fromDate,
                                    @Param("toDate") Instant toDate,
                                    Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM goods_receipts WHERE purchase_order_id = :id", nativeQuery = true)
    long countReceipts(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM goods_receipts WHERE purchase_order_id = :id AND status IN (0, 1)", nativeQuery = true)
    long countOpenOrConfirmedReceipts(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM goods_receipts WHERE purchase_order_id = :id AND status = 0", nativeQuery = true)
    long countPendingReceipts(@Param("id") Long id);

    @Query(value = """
        SELECT ri.purchase_order_item_id, SUM(ri.quantity_received)
        FROM receipt_items ri JOIN goods_receipts gr ON gr.id = ri.goods_receipt_id
        WHERE gr.purchase_order_id = :id AND gr.status = 1
        GROUP BY ri.purchase_order_item_id
        """, nativeQuery = true)
    List<Object[]> findConfirmedReceivedQuantities(@Param("id") Long id);
}
