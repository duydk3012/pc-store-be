package com.datn.pc_store.modules.analytics.repository;

import com.datn.pc_store.modules.order.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsRevenueRepository extends JpaRepository<Invoice, Long> {

    @Query(value = """
        SELECT 
            DATE_FORMAT(i.invoice_date, '%Y-%m-%d') AS timeBucket,
            DATE_FORMAT(i.invoice_date, '%d/%m') AS label,
            COALESCE(SUM(i.amount), 0) AS revenue,
            COUNT(DISTINCT i.id) AS orderCount,
            COALESCE(SUM(cogs_table.cost), 0) AS cogs
        FROM invoices i
        LEFT JOIN (
            SELECT 
                oi.order_id,
                SUM(ri.actual_unit_cost) AS cost
            FROM order_items oi
            JOIN order_item_serial ois ON ois.order_item_id = oi.id
            JOIN product_serials ps ON ois.product_serial_id = ps.id
            JOIN receipt_items ri ON ps.receipt_item_id = ri.id
            GROUP BY oi.order_id
        ) cogs_table ON cogs_table.order_id = i.order_id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        GROUP BY timeBucket, label
        ORDER BY timeBucket ASC
        """, nativeQuery = true)
    List<Object[]> queryRevenueAndProfitTrendDaily(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            DATE_FORMAT(i.invoice_date, '%Y-%m') AS timeBucket,
            DATE_FORMAT(i.invoice_date, '%m/%Y') AS label,
            COALESCE(SUM(i.amount), 0) AS revenue,
            COUNT(DISTINCT i.id) AS orderCount,
            COALESCE(SUM(cogs_table.cost), 0) AS cogs
        FROM invoices i
        LEFT JOIN (
            SELECT 
                oi.order_id,
                SUM(ri.actual_unit_cost) AS cost
            FROM order_items oi
            JOIN order_item_serial ois ON ois.order_item_id = oi.id
            JOIN product_serials ps ON ois.product_serial_id = ps.id
            JOIN receipt_items ri ON ps.receipt_item_id = ri.id
            GROUP BY oi.order_id
        ) cogs_table ON cogs_table.order_id = i.order_id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        GROUP BY timeBucket, label
        ORDER BY timeBucket ASC
        """, nativeQuery = true)
    List<Object[]> queryRevenueAndProfitTrendMonthly(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            b.id AS brandId,
            b.name AS brandName,
            COALESCE(SUM(oi.subtotal), 0) AS revenue
        FROM brands b
        JOIN products p ON b.id = p.brand_id
        JOIN order_items oi ON p.id = oi.product_id
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        GROUP BY b.id, b.name
        ORDER BY revenue DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> queryRevenueByBrandTopN(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime, @Param("limit") int limit);

    @Query(value = """
        SELECT 
            pm.id AS methodId,
            pm.name AS methodName,
            COALESCE(SUM(p.amount), 0) AS totalAmount
        FROM payment_methods pm
        JOIN payment p ON pm.id = p.payment_method_id
        JOIN orders o ON p.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        WHERE p.status = 1 
          AND i.status = 2
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        GROUP BY pm.id, pm.name
        ORDER BY totalAmount DESC
        """, nativeQuery = true)
    List<Object[]> queryRevenueByPaymentMethod(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
