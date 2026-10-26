package com.datn.pc_store.modules.goodsreceipt.repository;

import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    Optional<GoodsReceipt> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gr FROM GoodsReceipt gr WHERE gr.id = :id")
    Optional<GoodsReceipt> findForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(gr) FROM GoodsReceipt gr WHERE gr.code = :code AND (:id IS NULL OR gr.id <> :id)")
    long countByCode(@Param("code") String code, @Param("id") Long id);

    @Query("""
        SELECT gr FROM GoodsReceipt gr
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(gr.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(gr.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR gr.status = :status)
          AND (
                :filterMode = 0
             OR (:filterMode = 1 AND gr.createdBy = :currentUserId)
             OR (:filterMode = 2 AND (
                    gr.status IN :managerStatuses
                 OR ((gr.status = :draftStatus OR gr.status = :cancelledStatus) AND gr.createdBy = :currentUserId)
             ))
          )
          AND (:purchaseOrderId IS NULL OR gr.purchaseOrderId = :purchaseOrderId)
          AND (:fromDate IS NULL OR gr.receivedAt >= :fromDate)
          AND (:toDate IS NULL OR gr.receivedAt <= :toDate)
        """)
    Page<GoodsReceipt> searchGoodsReceipts(@Param("keyword") String keyword,
                                         @Param("status") GoodsReceiptStatus status,
                                         @Param("filterMode") int filterMode,
                                         @Param("currentUserId") Long currentUserId,
                                         @Param("managerStatuses") Collection<GoodsReceiptStatus> managerStatuses,
                                         @Param("draftStatus") GoodsReceiptStatus draftStatus,
                                         @Param("cancelledStatus") GoodsReceiptStatus cancelledStatus,
                                         @Param("purchaseOrderId") Long purchaseOrderId,
                                         @Param("fromDate") Instant fromDate,
                                         @Param("toDate") Instant toDate,
                                         Pageable pageable);
}
