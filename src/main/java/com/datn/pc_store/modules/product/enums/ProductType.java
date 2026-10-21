package com.datn.pc_store.modules.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Loại sản phẩm: COMPONENT (0 - linh kiện), LAPTOP (1), PC (2)")
public enum ProductType {
    COMPONENT(0), LAPTOP(1), PC(2);

    private final int value;

    ProductType(int value) {
        this.value = value;
    }

    public static ProductType fromValue(int value) {
        for (ProductType type : values()) {
            if (type.value == value) return type;
        }
        throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + value);
    }
}
