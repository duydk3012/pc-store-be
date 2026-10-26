package com.datn.pc_store.modules.goodsreceipt.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @Column(name = "purchase_order_item_id", nullable = false)
    private Long purchaseOrderItemId;

    @Column(name = "actual_unit_cost", nullable = false)
    private long actualUnitCost;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "quantity_accepted", nullable = false)
    private int quantityAccepted;

    @Column(name = "quantity_defective", nullable = false)
    private int quantityDefective;

    @OneToMany(mappedBy = "receiptItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ProductSerial> productSerials = new ArrayList<>();
}
