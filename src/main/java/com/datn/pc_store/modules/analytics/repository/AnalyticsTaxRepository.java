package com.datn.pc_store.modules.analytics.repository;

import com.datn.pc_store.modules.order.entity.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsTaxRepository extends JpaRepository<Invoice, Long> {

    @Query(value = """
        SELECT 
            COALESCE(SUM(amount), 0) AS totalRevenue,
            COUNT(id) AS invoiceCount
        FROM invoices
        WHERE status = 2 
          AND invoice_date >= :fromTime 
          AND invoice_date <= :toTime
        """, nativeQuery = true)
    List<Object[]> queryRevenueAndCountBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            QUARTER(CONVERT_TZ(invoice_date, '+00:00', '+07:00')) AS q,
            COALESCE(SUM(amount), 0) AS quarterRevenue,
            COUNT(id) AS invoiceCount
        FROM invoices
        WHERE status = 2 
          AND invoice_date >= :fromTime
          AND invoice_date <= :toTime
        GROUP BY q
        ORDER BY q ASC
        """, nativeQuery = true)
    List<Object[]> queryQuarterlyRevenueBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            i.id AS invoiceId,
            i.code AS invoiceCode,
            i.invoice_date AS invoiceDate,
            o.code AS orderCode,
            COALESCE(u.full_name, 'Khách vãng lai') AS customerName,
            COALESCE(u.phone_number, 'N/A') AS customerPhone,
            i.total_amount AS totalAmount,
            i.discount_amount AS discountAmount,
            i.amount AS amount,
            i.status AS statusVal
        FROM invoices i
        JOIN orders o ON i.order_id = o.id
        LEFT JOIN users u ON o.user_id = u.id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        ORDER BY i.invoice_date DESC
        """, nativeQuery = true)
    List<Object[]> queryTaxInvoicesPaged(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime, Pageable pageable);

    @Query(value = """
        SELECT COUNT(i.id)
        FROM invoices i
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        """, nativeQuery = true)
    long countTaxInvoicesBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = """
        SELECT 
            i.id AS invoiceId,
            i.code AS invoiceCode,
            i.invoice_date AS invoiceDate,
            o.code AS orderCode,
            COALESCE(u.full_name, 'Khách vãng lai') AS customerName,
            COALESCE(u.phone_number, 'N/A') AS customerPhone,
            i.total_amount AS totalAmount,
            i.discount_amount AS discountAmount,
            i.amount AS amount,
            i.status AS statusVal
        FROM invoices i
        JOIN orders o ON i.order_id = o.id
        LEFT JOIN users u ON o.user_id = u.id
        WHERE i.status = 2 
          AND i.invoice_date >= :fromTime 
          AND i.invoice_date <= :toTime
        ORDER BY i.invoice_date ASC
        """, nativeQuery = true)
    List<Object[]> queryAllTaxInvoicesForExport(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
