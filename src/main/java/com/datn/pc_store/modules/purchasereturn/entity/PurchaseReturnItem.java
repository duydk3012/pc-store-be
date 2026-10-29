package com.datn.pc_store.modules.purchasereturn.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "purchase_return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseReturnItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_return_id", nullable = false)
    private PurchaseReturn purchaseReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_serial_id")
    private ProductSerial productSerial;

    @Column(name = "unit_cost", nullable = false)
    @Builder.Default
    private long unitCost = 0L;
}
