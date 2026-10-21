package com.datn.pc_store.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_products", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "category_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
