package com.datn.pc_store.modules.goodsreceipt.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_serials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSerial extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_item_id")
    private ReceiptItem receiptItem;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(nullable = false)
    private ProductSerialStatus status;

    @Column(name = "is_terminated", nullable = false)
    @Builder.Default
    private boolean isTerminated = false;
}
