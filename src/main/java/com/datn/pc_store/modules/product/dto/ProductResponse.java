package com.datn.pc_store.product.dto;

import com.datn.pc_store.product.enums.ProductStatus;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String unit,
        ProductStatus status,
        Integer trackingType,
        String description,
        String slug,
        String imageUrl,
        Long categoryBrandsId,
        Integer isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
