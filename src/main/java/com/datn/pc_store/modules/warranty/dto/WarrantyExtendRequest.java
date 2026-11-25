package com.datn.pc_store.modules.warranty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WarrantyExtendRequest(
        @NotNull(message = "Số ngày gia hạn không được để trống")
        @Min(value = 1, message = "Số ngày gia hạn phải lớn hơn hoặc bằng 1")
        Integer additionalDays,
        String note
) {}

