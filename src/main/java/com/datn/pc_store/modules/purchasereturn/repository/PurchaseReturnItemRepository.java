package com.datn.pc_store.modules.purchasereturn.repository;

import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturnItem;
import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {

    List<PurchaseReturnItem> findByPurchaseReturnId(Long purchaseReturnId);

    @Query("""
        SELECT pri.productSerial.id FROM PurchaseReturnItem pri
        WHERE pri.productSerial.id IN :serialIds
          AND pri.purchaseReturn.status NOT IN :excludeStatuses
          AND (:excludeReturnId IS NULL OR pri.purchaseReturn.id <> :excludeReturnId)
    """)
    Set<Long> findSerialIdsInActiveReturns(
            @Param("serialIds") Collection<Long> serialIds,
            @Param("excludeStatuses") Collection<PurchaseReturnStatus> excludeStatuses,
            @Param("excludeReturnId") Long excludeReturnId
    );
}
