package com.datn.pc_store.modules.goodsreceipt.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái phiếu nhập: PENDING (0 - Chờ xử lý), CONFIRMED (1 - Đã xác nhận), REJECTED (2 - Từ chối), CANCELLED (3 - Đã hủy), DRAFT (4 - Nháp)")
public enum GoodsReceiptStatus {
    PENDING(0),
    CONFIRMED(1),
    REJECTED(2),
    CANCELLED(3),
    DRAFT(4);

    private final int value;

    GoodsReceiptStatus(int value) {
        this.value = value;
    }

    public static GoodsReceiptStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái phiếu nhập hàng không hợp lệ: " + value);
    }
}
