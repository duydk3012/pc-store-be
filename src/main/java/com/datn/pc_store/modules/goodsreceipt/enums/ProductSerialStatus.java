package com.datn.pc_store.modules.goodsreceipt.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái serial sản phẩm: GOOD_CONDITION (0 - Tốt), DEFECTIVE (1 - Lỗi), IN_STOCK (2 - Trong kho), PENDING_RETURN (3 - Chờ trả NCC), RETURNING (4 - Đang trả), RETURNED (5 - Đã trả), PACKING (6 - Đang đóng gói), DELIVERING (7 - Đang giao), SOLD (8 - Đã bán), USING_BY_TECHNICAL (9 - Kỹ thuật sử dụng)")
public enum ProductSerialStatus {
    GOOD_CONDITION(0),
    DEFECTIVE(1),
    IN_STOCK(2),
    PENDING_RETURN(3),
    RETURNING(4),
    RETURNED(5),
    PACKING(6),
    DELIVERING(7),
    SOLD(8),
    USING_BY_TECHNICAL(9);

    private final int value;

    ProductSerialStatus(int value) {
        this.value = value;
    }

    public static ProductSerialStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái serial sản phẩm không hợp lệ: " + value);
    }
}
