package com.datn.pc_store.modules.analytics.repository;

import com.datn.pc_store.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.code AS productCode,
            p.name AS productName,
            COALESCE((SELECT c.name FROM categories c JOIN category_products cp ON c.id = cp.category_id WHERE cp.product_id = p.id LIMIT 1), 'Chưa phân loại') AS categoryName,
            COALESCE(SUM(oi.quantity), 0) AS qtySold,
            COALESCE(SUM(oi.subtotal), 0) AS revenue,
            COALESCE(SUM(cogs_table.cost), 0) AS cogs,
            (COALESCE(SUM(oi.subtotal), 0) - COALESCE(SUM(cogs_table.cost), 0)) AS grossProfit
        FROM products p
        JOIN order_items oi ON p.id = oi.product_id
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        LEFT JOIN (
            SELECT 
                ois.order_item_id,
                SUM(ri.actual_unit_cost) AS cost
            FROM order_item_serial ois
            JOIN product_serials ps ON ois.product_serial_id = ps.id
            JOIN receipt_items ri ON ps.receipt_item_id = ri.id
            GROUP BY ois.order_item_id
        ) cogs_table ON cogs_table.order_item_id = oi.id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        GROUP BY p.id, p.code, p.name
        ORDER BY qtySold DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> queryTopSellingProducts(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime, @Param("limit") int limit);

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.code AS productCode,
            p.name AS productName,
            COALESCE((SELECT c.name FROM categories c JOIN category_products cp ON c.id = cp.category_id WHERE cp.product_id = p.id LIMIT 1), 'Chưa phân loại') AS categoryName,
            COALESCE(SUM(oi.quantity), 0) AS qtySold,
            COALESCE(SUM(oi.subtotal), 0) AS revenue,
            COALESCE(SUM(cogs_table.cost), 0) AS cogs,
            (COALESCE(SUM(oi.subtotal), 0) - COALESCE(SUM(cogs_table.cost), 0)) AS grossProfit
        FROM products p
        JOIN order_items oi ON p.id = oi.product_id
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        LEFT JOIN (
            SELECT 
                ois.order_item_id,
                SUM(ri.actual_unit_cost) AS cost
            FROM order_item_serial ois
            JOIN product_serials ps ON ois.product_serial_id = ps.id
            JOIN receipt_items ri ON ps.receipt_item_id = ri.id
            GROUP BY ois.order_item_id
        ) cogs_table ON cogs_table.order_item_id = oi.id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        GROUP BY p.id, p.code, p.name
        ORDER BY grossProfit DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> queryTopGrossProfitProducts(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime, @Param("limit") int limit);

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.code AS productCode,
            p.name AS productName,
            COALESCE((SELECT c.name FROM categories c JOIN category_products cp ON c.id = cp.category_id WHERE cp.product_id = p.id LIMIT 1), 'Chưa phân loại') AS categoryName,
            COALESCE((SELECT b.name FROM brands b WHERE b.id = p.brand_id), 'Chưa rõ') AS brandName,
            COALESCE(SUM(oi.quantity), 0) AS qtySold,
            COALESCE(SUM(oi.subtotal), 0) AS revenue,
            COALESCE(SUM(cogs_table.cost), 0) AS cogs,
            (COALESCE(SUM(oi.subtotal), 0) - COALESCE(SUM(cogs_table.cost), 0)) AS grossProfit,
            (
                SELECT COUNT(ps.id)
                FROM product_serials ps
                WHERE ps.product_id = p.id AND ps.status IN (0, 2) AND ps.is_terminated = 0
            ) AS currentStock,
            (
                SELECT COUNT(wr.id)
                FROM warranty_request wr
                JOIN warranty w ON wr.warranty_id = w.id
                JOIN order_item_serial ois ON w.order_item_serial_id = ois.id
                JOIN product_serials ps ON ois.product_serial_id = ps.id
                WHERE ps.product_id = p.id
            ) AS warrantyCount
        FROM products p
        LEFT JOIN order_items oi ON p.id = oi.product_id
        LEFT JOIN orders o ON oi.order_id = o.id
        LEFT JOIN invoices i ON i.order_id = o.id AND i.status = 2 AND i.invoice_date >= :fromTime AND i.invoice_date <= :toTime
        LEFT JOIN (
            SELECT 
                ois.order_item_id,
                SUM(ri.actual_unit_cost) AS cost
            FROM order_item_serial ois
            JOIN product_serials ps ON ois.product_serial_id = ps.id
            JOIN receipt_items ri ON ps.receipt_item_id = ri.id
            GROUP BY ois.order_item_id
        ) cogs_table ON cogs_table.order_item_id = oi.id
        WHERE p.is_deleted = 0
        GROUP BY p.id, p.code, p.name, p.brand_id
        ORDER BY revenue DESC
        LIMIT 100
        """, nativeQuery = true)
    List<Object[]> queryProductPerformanceMatrix(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
