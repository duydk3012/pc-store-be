package com.datn.pc_store.modules.warranty.dto;

import java.time.Instant;

/**
 * Thông tin bảo hành công khai (chỉ chứa dữ liệu không nhạy cảm) khi tra cứu theo serial.
 */
public record PublicWarrantyResponse(
        String serialNumber,
        Long productId,
        String productName,
        String productCode,
        String brandName,
        String policyName,
        int durationDays,
        Instant startDate,
        Instant endDate,
        boolean active,
        String status,
        long remainingDays
) {}
