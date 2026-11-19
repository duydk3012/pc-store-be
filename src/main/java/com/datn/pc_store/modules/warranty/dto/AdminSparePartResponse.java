package com.datn.pc_store.modules.warranty.dto;

import com.datn.pc_store.modules.warranty.enums.WarrantyPartStatus;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartType;

import java.time.Instant;
import java.util.List;

public record AdminSparePartResponse(
        Long id,
        Long warrantyRequestId,
        String requestCode,
        WarrantyPartType type,
        String typeName,
        Long productId,
        String productName,
        String productCode,
        String productCategory,
        int quantity,
        long originalPrice,
        int warrantyCoveragePercent,
        long unitCost,
        long totalCost,
        WarrantyPartStatus status,
        List<String> serialNumbers,
        String assignedTechnician,
        Instant createdDate,
        Instant lastModifiedDate
) {}
