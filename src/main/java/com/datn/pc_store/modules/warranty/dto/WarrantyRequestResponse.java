package com.datn.pc_store.modules.warranty.dto;

import com.datn.pc_store.modules.warranty.enums.WarrantyPartStatus;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartType;
import com.datn.pc_store.modules.warranty.enums.WarrantyRequestStatus;

import java.util.List;

public record WarrantyRequestResponse(
        Long id,
        String requestCode,
        Long warrantyId,
        Long customerId,
        String issueDescription,
        WarrantyRequestStatus status,
        long totalCost,
        List<Part> parts
) {
    public record Part(
            Long id,
            Long productId,
            WarrantyPartType type,
            int quantity,
            long originalPrice,
            int warrantyCoveragePercent,
            long unitCost,
            long subtotal,
            WarrantyPartStatus status,
            List<Long> productSerialIds
    ) {}
}
