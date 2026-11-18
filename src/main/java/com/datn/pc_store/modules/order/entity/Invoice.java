package com.datn.pc_store.modules.order.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import com.datn.pc_store.modules.order.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @Column(name = "invoice_date", nullable = false)
    private Instant invoiceDate;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private long totalAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private long discountAmount = 0L;

    @Column(name = "tax_amount", nullable = false)
    @Builder.Default
    private long taxAmount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private long amount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;
}
