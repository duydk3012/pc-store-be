package com.datn.pc_store.modules.purchaseorder.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái đơn nhập: DRAFT (0 - Nháp), PENDING (1 - Chờ xử lý), APPROVED (2 - Đã duyệt), RECEIVED (3 - Đã nhận), CANCELLED (4 - Đã hủy)")
public enum PurchaseOrderStatus {
    DRAFT(0), PENDING(1), APPROVED(2), RECEIVED(3), CANCELLED(4);

    private final int value;

    PurchaseOrderStatus(int value) {
        this.value = value;
    }

    public static PurchaseOrderStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái đơn nhập hàng không hợp lệ: " + value);
    }
}
