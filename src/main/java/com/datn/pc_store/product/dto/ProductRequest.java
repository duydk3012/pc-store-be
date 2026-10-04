package com.datn.pc_store.product.dto;

import com.datn.pc_store.product.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 30) String unit,
        ProductStatus status,
        @NotNull Integer trackingType,
        @Size(max = 255) String description,
        @Size(max = 50) String slug,
        @Size(max = 50) String imageUrl,
        Long categoryBrandsId
) {
}
