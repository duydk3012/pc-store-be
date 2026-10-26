package com.datn.pc_store.modules.goodsreceipt.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String name;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(nullable = false)
    @Builder.Default
    private GoodsReceiptStatus status = GoodsReceiptStatus.PENDING;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ReceiptItem> items = new ArrayList<>();
}
