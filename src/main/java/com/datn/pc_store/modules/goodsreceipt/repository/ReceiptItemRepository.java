package com.datn.pc_store.modules.goodsreceipt.repository;

import com.datn.pc_store.modules.goodsreceipt.entity.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
    List<ReceiptItem> findByGoodsReceiptId(Long goodsReceiptId);

    List<ReceiptItem> findByPurchaseOrderItemId(Long purchaseOrderItemId);
}
