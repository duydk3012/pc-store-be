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
        WITH RECURSIVE CategoryHierarchy AS (
            SELECT id AS category_id, name AS category_name, id AS root_id, name AS root_name, 0 AS depth
            FROM categories
            WHERE parent_id IS NULL
            UNION ALL
            SELECT c.id AS category_id, c.name AS category_name, ch.root_id, ch.root_name, ch.depth + 1 AS depth
            FROM categories c
            JOIN CategoryHierarchy ch ON c.parent_id = ch.category_id
        ),
        ProductPrimaryCategory AS (
            SELECT 
                cp.product_id,
                ch.category_id AS primary_cat_id,
                ch.category_name AS primary_cat_name,
                ch.root_id,
                ch.root_name,
                ROW_NUMBER() OVER (PARTITION BY cp.product_id ORDER BY cp.is_primary DESC, ch.depth DESC, cp.category_id ASC) AS rn
            FROM category_products cp
            JOIN CategoryHierarchy ch ON cp.category_id = ch.category_id
        ),
        ProductSecondaryChild AS (
            SELECT 
                cp.product_id,
                ch.root_id,
                ch.category_id AS sec_cat_id,
                ch.category_name AS sec_cat_name,
                ROW_NUMBER() OVER (PARTITION BY cp.product_id, ch.root_id ORDER BY ch.depth DESC, cp.category_id ASC) AS rn
            FROM category_products cp
            JOIN CategoryHierarchy ch ON cp.category_id = ch.category_id
            WHERE cp.is_primary = 0 AND ch.category_id <> ch.root_id
        ),
        ProductMapped AS (
            SELECT 
                ppc.product_id,
                ppc.root_id,
                ppc.root_name,
                CASE 
                    WHEN ppc.primary_cat_id <> ppc.root_id THEN ppc.primary_cat_id
                    WHEN psc.sec_cat_id IS NOT NULL THEN psc.sec_cat_id
                    ELSE NULL
                END AS child_id,
                CASE 
                    WHEN ppc.primary_cat_id <> ppc.root_id THEN ppc.primary_cat_name
                    WHEN psc.sec_cat_id IS NOT NULL THEN psc.sec_cat_name
                    ELSE NULL
                END AS child_name
            FROM ProductPrimaryCategory ppc
            LEFT JOIN ProductSecondaryChild psc 
                ON ppc.product_id = psc.product_id AND ppc.root_id = psc.root_id AND psc.rn = 1
            WHERE ppc.rn = 1
        )
        SELECT 
            COALESCE(pm.root_id, 0) AS rootCategoryId,
            COALESCE(pm.root_name, 'Chưa phân loại') AS rootCategoryName,
            pm.child_id AS childCategoryId,
            pm.child_name AS childCategoryName,
            COALESCE(SUM(oi.subtotal), 0) AS revenue
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        LEFT JOIN ProductMapped pm ON oi.product_id = pm.product_id
        WHERE i.status = 2 AND i.invoice_date >= :fromTime AND i.invoice_date <= :toTime
        GROUP BY COALESCE(pm.root_id, 0), COALESCE(pm.root_name, 'Chưa phân loại'), pm.child_id, pm.child_name
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> queryRevenueByCategoryHierarchy(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        WITH RECURSIVE CategoryHierarchy AS (
            SELECT id AS category_id, name AS category_name, id AS root_id, name AS root_name, 0 AS depth
            FROM categories
            WHERE parent_id IS NULL
            UNION ALL
            SELECT c.id AS category_id, c.name AS category_name, ch.root_id, ch.root_name, ch.depth + 1 AS depth
            FROM categories c
            JOIN CategoryHierarchy ch ON c.parent_id = ch.category_id
        ),
        ProductAllRootCategories AS (
            SELECT 
                cp.product_id,
                ch.root_id,
                ch.root_name,
                ch.category_id,
                ch.category_name,
                ROW_NUMBER() OVER (
                    PARTITION BY cp.product_id, ch.root_id 
                    ORDER BY ch.depth DESC, cp.is_primary DESC, cp.category_id ASC
                ) AS rn
            FROM category_products cp
            JOIN CategoryHierarchy ch ON cp.category_id = ch.category_id
        )
        SELECT 
            COALESCE(parc.root_id, 0) AS rootCategoryId,
            COALESCE(parc.root_name, 'Chưa phân loại') AS rootCategoryName,
            CASE WHEN parc.category_id <> parc.root_id THEN parc.category_id ELSE NULL END AS childCategoryId,
            CASE WHEN parc.category_id <> parc.root_id THEN parc.category_name ELSE NULL END AS childCategoryName,
            COALESCE(SUM(oi.subtotal), 0) AS revenue
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN invoices i ON i.order_id = o.id
        LEFT JOIN ProductAllRootCategories parc ON oi.product_id = parc.product_id AND parc.rn = 1
        WHERE i.status = 2 AND i.invoice_date >= :fromTime AND i.invoice_date <= :toTime
        GROUP BY 
            COALESCE(parc.root_id, 0), 
            COALESCE(parc.root_name, 'Chưa phân loại'),
            CASE WHEN parc.category_id <> parc.root_id THEN parc.category_id ELSE NULL END,
            CASE WHEN parc.category_id <> parc.root_id THEN parc.category_name ELSE NULL END
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> queryRevenueByAllLinkedCategories(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            status,
            COUNT(id) AS statusCount
        FROM orders
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> queryOrderStatusCounts();
}
