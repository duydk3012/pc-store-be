package com.datn.pc_store.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 25) String name,
        @NotBlank @Size(max = 50) String slug
) {
}
