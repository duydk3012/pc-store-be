package com.datn.pc_store.modules.product.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static ProductStatus fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (ProductStatus status : values()) {
            if (status.name().equalsIgnoreCase(str) || String.valueOf(status.value).equals(str)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ: " + str);
    }
}
