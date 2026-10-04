package com.datn.pc_store.category.dto;

import com.datn.pc_store.category.entity.Category;
import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Integer isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getIsDeleted(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
