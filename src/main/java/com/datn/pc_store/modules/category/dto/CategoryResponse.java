package com.datn.pc_store.category.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Integer isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
