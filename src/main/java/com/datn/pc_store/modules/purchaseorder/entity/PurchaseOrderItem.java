package com.datn.pc_store.modules.purchaseorder.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity_order", nullable = false)
    private int quantityOrder;

    @Column(name = "unit_cost", nullable = false)
    private long unitCost;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(nullable = false)
    private long subtotal;
}
