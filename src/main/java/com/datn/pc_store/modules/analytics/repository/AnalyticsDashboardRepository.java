package com.datn.pc_store.modules.analytics.repository;

import com.datn.pc_store.modules.order.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsDashboardRepository extends JpaRepository<Invoice, Long> {

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE status = 2 AND invoice_date >= :fromTime AND invoice_date <= :toTime", nativeQuery = true)
    long sumRevenueBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = "SELECT COUNT(id) FROM invoices WHERE status = 2 AND invoice_date >= :fromTime AND invoice_date <= :toTime", nativeQuery = true)
    long countPaidInvoicesBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        WHERE i.status = 2 AND i.invoice_date >= :fromTime AND i.invoice_date <= :toTime
        """, nativeQuery = true)
    long sumProductsSoldBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT COALESCE(SUM(ri.actual_unit_cost), 0)
        FROM invoices i
        JOIN orders o ON i.order_id = o.id
        JOIN order_items oi ON oi.order_id = o.id
        JOIN order_item_serial ois ON ois.order_item_id = oi.id
        JOIN product_serials ps ON ois.product_serial_id = ps.id
        JOIN receipt_items ri ON ps.receipt_item_id = ri.id
        WHERE i.status = 2 AND i.invoice_date >= :fromTime AND i.invoice_date <= :toTime
        """, nativeQuery = true)
    long sumCogsBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT COALESCE(SUM(ri.actual_unit_cost), 0)
        FROM product_serials ps
        JOIN receipt_items ri ON ps.receipt_item_id = ri.id
        WHERE ps.status IN (0, 2) AND ps.is_terminated = 0
        """, nativeQuery = true)
    long sumCurrentSellableInventoryValue();

    @Query(value = """
        SELECT COUNT(p.id)
        FROM products p
        WHERE p.is_deleted = 0
          AND (
            SELECT COUNT(ps.id)
            FROM product_serials ps
            WHERE ps.product_id = p.id
              AND ps.status IN (0, 2)
              AND ps.is_terminated = 0
          ) <= :threshold
        """, nativeQuery = true)
    long countLowStockProducts(@Param("threshold") int threshold);

    @Query(value = "SELECT COUNT(id) FROM orders WHERE status IN (0, 1, 3)", nativeQuery = true)
    long countProcessingOrders();

    @Query(value = """
        SELECT 
            DATE_FORMAT(invoice_date, '%Y-%m-%d') AS timeBucket,
            DATE_FORMAT(invoice_date, '%d/%m') AS label,
            COALESCE(SUM(amount), 0) AS revenue,
            COUNT(id) AS orderCount
        FROM invoices
        WHERE status = 2 AND invoice_date >= :fromTime AND invoice_date <= :toTime
        GROUP BY timeBucket, label
        ORDER BY timeBucket ASC
        """, nativeQuery = true)
    List<Object[]> queryRevenueAndOrderTrendDaily(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            COALESCE(SUM(oi.subtotal), 0) AS revenue
        FROM categories c
        JOIN category_products cp ON c.id = cp.category_id
        JOIN order_items oi ON cp.product_id = oi.product_id
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        WHERE i.status = 2 AND i.invoice_date >= :fromTime AND i.invoice_date <= :toTime
        GROUP BY c.id, c.name
        ORDER BY revenue DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> queryRevenueByCategoryTopN(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime, @Param("limit") int limit);

    @Query(value = """
        SELECT 
            status,
            COUNT(id) AS statusCount
        FROM orders
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> queryOrderStatusCounts();
}
