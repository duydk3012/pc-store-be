package com.datn.pc_store.modules.analytics.repository;

import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsPurchaseRepository extends JpaRepository<GoodsReceipt, Long> {

    @Query(value = """
        SELECT 
            DATE_FORMAT(gr.received_at, '%Y-%m-%d') AS timeBucket,
            DATE_FORMAT(gr.received_at, '%d/%m') AS label,
            COALESCE(SUM(ri.actual_unit_cost * ri.quantity_received), 0) AS importValue,
            COUNT(DISTINCT gr.id) AS receiptCount
        FROM goods_receipts gr
        JOIN receipt_items ri ON gr.id = ri.goods_receipt_id
        WHERE gr.status = 1 
          AND gr.received_at >= :fromTime 
          AND gr.received_at <= :toTime
        GROUP BY timeBucket, label
        ORDER BY timeBucket ASC
        """, nativeQuery = true)
    List<Object[]> queryPurchasingTrendDaily(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            DATE_FORMAT(gr.received_at, '%Y-%m') AS timeBucket,
            DATE_FORMAT(gr.received_at, '%m/%Y') AS label,
            COALESCE(SUM(ri.actual_unit_cost * ri.quantity_received), 0) AS importValue,
            COUNT(DISTINCT gr.id) AS receiptCount
        FROM goods_receipts gr
        JOIN receipt_items ri ON gr.id = ri.goods_receipt_id
        WHERE gr.status = 1 
          AND gr.received_at >= :fromTime 
          AND gr.received_at <= :toTime
        GROUP BY timeBucket, label
        ORDER BY timeBucket ASC
        """, nativeQuery = true)
    List<Object[]> queryPurchasingTrendMonthly(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            s.id AS supplierId,
            s.name AS supplierName,
            s.code AS supplierCode,
            COALESCE(SUM(ri.actual_unit_cost * ri.quantity_received), 0) AS totalImportValue,
            COUNT(DISTINCT gr.id) AS receiptCount,
            COALESCE(SUM(ri.quantity_received), 0) AS totalQuantityReceived,
            COALESCE(SUM(ri.quantity_accepted), 0) AS acceptedQuantity,
            COALESCE(SUM(ri.quantity_defective), 0) AS defectiveQuantity
        FROM suppliers s
        JOIN purchase_orders po ON s.id = po.supplier_id
        JOIN goods_receipts gr ON po.id = gr.purchase_order_id
        JOIN receipt_items ri ON gr.id = ri.goods_receipt_id
        WHERE gr.status = 1 
          AND gr.received_at >= :fromTime 
          AND gr.received_at <= :toTime
        GROUP BY s.id, s.name, s.code
        ORDER BY totalImportValue DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> queryPurchasingBySupplierTopN(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime, @Param("limit") int limit);

    @Query(value = """
        SELECT 
            gr.status AS statusVal,
            COUNT(gr.id) AS countVal
        FROM goods_receipts gr
        WHERE gr.created_date >= :fromTime 
          AND gr.created_date <= :toTime
        GROUP BY gr.status
        """, nativeQuery = true)
    List<Object[]> queryReceiptStatusCounts(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
