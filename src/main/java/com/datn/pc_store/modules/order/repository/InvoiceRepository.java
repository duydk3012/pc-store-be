package com.datn.pc_store.modules.order.repository;

import com.datn.pc_store.modules.order.entity.Invoice;
import com.datn.pc_store.modules.order.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByCode(String code);

    Optional<Invoice> findFirstByOrderIdOrderByCreatedDateDesc(Long orderId);

    boolean existsByCode(String code);

    boolean existsByOrderIdAndStatusNot(Long orderId, InvoiceStatus status);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.orderId = :orderId AND i.status = :status")
    long countByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") InvoiceStatus status);
}
