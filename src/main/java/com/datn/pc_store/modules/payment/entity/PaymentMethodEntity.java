package com.datn.pc_store.modules.payment.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import com.datn.pc_store.modules.payment.enums.PaymentMethodStatus;
import com.datn.pc_store.modules.payment.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "payment_methods")
@SQLRestriction("is_deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private PaymentMethodType type = PaymentMethodType.CASH;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(nullable = false)
    @Builder.Default
    private PaymentMethodStatus status = PaymentMethodStatus.ACTIVE;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
