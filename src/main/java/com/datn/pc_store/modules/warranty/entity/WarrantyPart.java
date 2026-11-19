package com.datn.pc_store.modules.warranty.entity;

import com.datn.pc_store.common.entity.BaseEntity;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartStatus;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartType;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartTypeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "warranty_parts")
@Getter
@Setter
public class WarrantyPart extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warrantyRequestId;

    @Convert(converter = WarrantyPartTypeConverter.class)
    @Column(nullable = false)
    private WarrantyPartType type = WarrantyPartType.WARRANTY;

    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private long unitCost = 0L;

    @Column(name = "original_price", nullable = false)
    private long originalPrice = 0L;

    @Column(name = "warranty_coverage_percent", nullable = false)
    private int warrantyCoveragePercent = 100;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private WarrantyPartStatus status = WarrantyPartStatus.PENDING;
}
