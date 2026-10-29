package com.datn.pc_store.modules.purchasereturn.repository;

import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturn;
import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    Optional<PurchaseReturn> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pr FROM PurchaseReturn pr WHERE pr.id = :id")
    Optional<PurchaseReturn> findForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(pr) FROM PurchaseReturn pr WHERE pr.code = :code AND (:id IS NULL OR pr.id <> :id)")
    long countByCode(@Param("code") String code, @Param("id") Long id);

    @Query("""
        SELECT pr FROM PurchaseReturn pr
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(pr.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(pr.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR pr.status = :status)
          AND (:supplierId IS NULL OR pr.supplierId = :supplierId)
          AND (
                :filterMode = 0
             OR (:filterMode = 1 AND pr.createdBy = :currentUserId)
             OR (:filterMode = 2 AND (
                    pr.status IN :managerStatuses
                 OR ((pr.status = :draftStatus OR pr.status = :cancelledStatus) AND pr.createdBy = :currentUserId)
             ))
          )
          AND (:fromDate IS NULL OR COALESCE(pr.returnDate, pr.createdDate) >= :fromDate)
          AND (:toDate IS NULL OR COALESCE(pr.returnDate, pr.createdDate) <= :toDate)
        """)
    Page<PurchaseReturn> searchReturns(
            @Param("keyword") String keyword,
            @Param("status") PurchaseReturnStatus status,
            @Param("supplierId") Long supplierId,
            @Param("filterMode") int filterMode,
            @Param("currentUserId") Long currentUserId,
            @Param("managerStatuses") Collection<PurchaseReturnStatus> managerStatuses,
            @Param("draftStatus") PurchaseReturnStatus draftStatus,
            @Param("cancelledStatus") PurchaseReturnStatus cancelledStatus,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query("""
        SELECT ps FROM ProductSerial ps
        JOIN ps.receiptItem ri
        JOIN ri.goodsReceipt gr
        JOIN PurchaseOrder po ON po.id = gr.purchaseOrderId
        WHERE po.supplierId = :supplierId
          AND ps.isTerminated = false
          AND ps.status IN :eligibleStatuses
          AND (:productId IS NULL OR ps.productId = :productId)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(ps.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND ps.id NOT IN (
              SELECT pri.productSerial.id FROM PurchaseReturnItem pri
              WHERE pri.productSerial IS NOT NULL
                AND pri.purchaseReturn.status NOT IN :excludeReturnStatuses
                AND (:excludeReturnId IS NULL OR pri.purchaseReturn.id <> :excludeReturnId)
          )
        ORDER BY ps.createdDate DESC
    """)
    List<ProductSerial> findAvailableSerialsForReturn(
            @Param("supplierId") Long supplierId,
            @Param("eligibleStatuses") Collection<ProductSerialStatus> eligibleStatuses,
            @Param("excludeReturnStatuses") Collection<PurchaseReturnStatus> excludeReturnStatuses,
            @Param("excludeReturnId") Long excludeReturnId,
            @Param("productId") Long productId,
            @Param("keyword") String keyword
    );

    @Query("""
        SELECT po.supplierId FROM ProductSerial ps
        JOIN ps.receiptItem ri
        JOIN ri.goodsReceipt gr
        JOIN PurchaseOrder po ON po.id = gr.purchaseOrderId
        WHERE ps.id = :serialId
    """)
    Optional<Long> findSupplierIdBySerialId(@Param("serialId") Long serialId);

    @Query("""
        SELECT ps FROM ProductSerial ps
        JOIN FETCH ps.receiptItem ri
        JOIN FETCH ri.goodsReceipt gr
        JOIN PurchaseOrder po ON po.id = gr.purchaseOrderId
        WHERE ps.serialNumber = :serialNumber
          AND ps.isTerminated = false
    """)
    List<ProductSerial> findActiveSerialsWithReceiptBySerialNumber(@Param("serialNumber") String serialNumber);
}
