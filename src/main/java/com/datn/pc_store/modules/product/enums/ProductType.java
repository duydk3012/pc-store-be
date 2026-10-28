package com.datn.pc_store.modules.product.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Loại sản phẩm: OTHER (0 - sản phẩm khác), PC (1 - PC)")
public enum ProductType {
    OTHER(0), PC(1);

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

    @JsonCreator
    public static ProductType fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (ProductType type : values()) {
            if (type.name().equalsIgnoreCase(str) || String.valueOf(type.value).equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + str);
    }
}
