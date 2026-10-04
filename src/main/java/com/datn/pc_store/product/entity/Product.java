package com.datn.pc_store.product.entity;

import com.datn.pc_store.product.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 30)
    private String unit;

    @Convert(converter = ProductStatusConverter.class)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "tracking_type", nullable = false)
    private Integer trackingType;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String slug;

    @Column(name = "image_url", length = 50)
    private String imageUrl;

    @Column(name = "category_brandsid")
    private Long categoryBrandsId;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (isDeleted == null) {
            isDeleted = 0;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
