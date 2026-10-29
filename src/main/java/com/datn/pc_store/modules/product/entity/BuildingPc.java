package com.datn.pc_store.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "building_pc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingPc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pc_id", nullable = false)
    private Long pcId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}
