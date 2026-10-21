package com.datn.pc_store.modules.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái sản phẩm: INACTIVE (0), ACTIVE (1), OUT_OF_STOCK (2)")
public enum ProductStatus {
    INACTIVE(0), ACTIVE(1), OUT_OF_STOCK(2);

    private final int value;

    ProductStatus(int value) {
        this.value = value;
    }

    public static ProductStatus fromValue(int value) {
        for (ProductStatus status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ: " + value);
    }
}
