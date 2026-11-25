package com.datn.pc_store.modules.analytics.repository;

import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsInventoryRepository extends JpaRepository<ProductSerial, Long> {

    @Query(value = """
        SELECT 
            COUNT(ps.id) AS totalSerials,
            COALESCE(SUM(ri.actual_unit_cost), 0) AS totalValue,
            SUM(CASE WHEN ps.status IN (0, 2) AND ps.is_terminated = 0 THEN 1 ELSE 0 END) AS sellableCount,
            COALESCE(SUM(CASE WHEN ps.status IN (0, 2) AND ps.is_terminated = 0 THEN ri.actual_unit_cost ELSE 0 END), 0) AS sellableValue,
            SUM(CASE WHEN ps.status IN (1, 3, 4, 5) AND ps.is_terminated = 0 THEN 1 ELSE 0 END) AS defectiveCount,
            SUM(CASE WHEN ps.status IN (6, 7) AND ps.is_terminated = 0 THEN 1 ELSE 0 END) AS committedCount,
            SUM(CASE WHEN ps.status = 9 AND ps.is_terminated = 0 THEN 1 ELSE 0 END) AS technicalCount
        FROM product_serials ps
        LEFT JOIN receipt_items ri ON ps.receipt_item_id = ri.id
        WHERE ps.is_terminated = 0
        """, nativeQuery = true)
    List<Object[]> queryInventoryOverviewSummary();

    @Query(value = """
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            COUNT(ps.id) AS stockCount,
            COALESCE(SUM(ri.actual_unit_cost), 0) AS stockValue
        FROM categories c
        JOIN category_products cp ON c.id = cp.category_id
        JOIN products p ON cp.product_id = p.id
        JOIN product_serials ps ON p.id = ps.product_id
        LEFT JOIN receipt_items ri ON ps.receipt_item_id = ri.id
        WHERE ps.status IN (0, 2) 
          AND ps.is_terminated = 0
          AND p.is_deleted = 0
        GROUP BY c.id, c.name
        ORDER BY stockValue DESC
        """, nativeQuery = true)
    List<Object[]> queryStockByCategory();

    @Query(value = """
        SELECT 
            ps.status AS statusVal,
            COUNT(ps.id) AS countVal
        FROM product_serials ps
        WHERE ps.is_terminated = 0
        GROUP BY ps.status
        """, nativeQuery = true)
    List<Object[]> querySerialStatusCounts();

    @Query(value = """
        SELECT 
            CASE 
                WHEN DATEDIFF(NOW(), COALESCE(gr.received_at, ps.created_date)) <= 30 THEN 'DAYS_0_TO_30'
                WHEN DATEDIFF(NOW(), COALESCE(gr.received_at, ps.created_date)) <= 60 THEN 'DAYS_31_TO_60'
                WHEN DATEDIFF(NOW(), COALESCE(gr.received_at, ps.created_date)) <= 90 THEN 'DAYS_61_TO_90'
                ELSE 'DAYS_OVER_90'
            END AS bracket,
            COUNT(ps.id) AS qty,
            COALESCE(SUM(ri.actual_unit_cost), 0) AS val
        FROM product_serials ps
        LEFT JOIN receipt_items ri ON ps.receipt_item_id = ri.id
        LEFT JOIN goods_receipts gr ON ri.goods_receipt_id = gr.id
        WHERE ps.status IN (0, 2) 
          AND ps.is_terminated = 0
        GROUP BY bracket
        """, nativeQuery = true)
    List<Object[]> queryInventoryAging();

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.code AS productCode,
            p.name AS productName,
            COALESCE((SELECT c.name FROM categories c JOIN category_products cp ON c.id = cp.category_id WHERE cp.product_id = p.id LIMIT 1), 'Chưa phân loại') AS categoryName,
            COUNT(DISTINCT ps.id) AS sellableQuantity,
            s.name AS supplierName
        FROM products p
        LEFT JOIN product_serials ps ON p.id = ps.product_id AND ps.status IN (0, 2) AND ps.is_terminated = 0
        LEFT JOIN purchase_order_items poi ON poi.product_id = p.id
        LEFT JOIN purchase_orders po ON poi.purchase_order_id = po.id
        LEFT JOIN suppliers s ON po.supplier_id = s.id
        WHERE p.is_deleted = 0
        GROUP BY p.id, p.code, p.name, s.name
        HAVING COUNT(DISTINCT ps.id) <= :threshold
        ORDER BY sellableQuantity ASC
        LIMIT 50
        """, nativeQuery = true)
    List<Object[]> queryLowStockAlerts(@Param("threshold") int threshold);

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.code AS productCode,
            p.name AS productName,
            COALESCE((SELECT c.name FROM categories c JOIN category_products cp ON c.id = cp.category_id WHERE cp.product_id = p.id LIMIT 1), 'Chưa phân loại') AS categoryName,
            COUNT(DISTINCT ps.id) AS sellableQuantity,
            MAX(o.created_date) AS lastSoldDate,
            COALESCE(DATEDIFF(NOW(), MAX(o.created_date)), DATEDIFF(NOW(), MIN(ps.created_date)), 0) AS daysWithoutSale
        FROM products p
        JOIN product_serials ps ON p.id = ps.product_id AND ps.status IN (0, 2) AND ps.is_terminated = 0
        LEFT JOIN order_items oi ON p.id = oi.product_id
        LEFT JOIN orders o ON oi.order_id = o.id AND o.status = 4
        WHERE p.is_deleted = 0
        GROUP BY p.id, p.code, p.name
        HAVING daysWithoutSale >= :thresholdDays
        ORDER BY daysWithoutSale DESC
        LIMIT 50
        """, nativeQuery = true)
    List<Object[]> querySlowMovingProducts(@Param("thresholdDays") int thresholdDays);
}
