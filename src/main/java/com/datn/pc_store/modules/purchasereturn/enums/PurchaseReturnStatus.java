package com.datn.pc_store.modules.purchasereturn.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái phiếu hoàn hàng NCC: PENDING (0 - Chờ xử lý/Chờ xuất), DELIVERING (1 - Đang giao trả), RETURNED (2 - Đã hoàn trả), CANCELLED (3 - Đã hủy), DRAFT (4 - Nháp)")
public enum PurchaseReturnStatus {
    PENDING(0),
    DELIVERING(1),
    RETURNED(2),
    CANCELLED(3),
    DRAFT(4);

    private final int value;

    PurchaseReturnStatus(int value) {
        this.value = value;
    }

    public static PurchaseReturnStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái phiếu hoàn trả không hợp lệ: " + value);
    }

    @JsonCreator
    public static PurchaseReturnStatus fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (var status : values()) {
            if (status.name().equalsIgnoreCase(str) || String.valueOf(status.value).equals(str)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái phiếu hoàn trả không hợp lệ: " + str);
    }
}
