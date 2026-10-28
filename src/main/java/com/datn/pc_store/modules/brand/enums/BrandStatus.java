package com.datn.pc_store.modules.brand.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái thương hiệu: INACTIVE (0), ACTIVE (1)")
public enum BrandStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    BrandStatus(int value) {
        this.value = value;
    }

    public static BrandStatus fromValue(int value) {
        for (BrandStatus s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        throw new IllegalArgumentException("Trạng thái thương hiệu không hợp lệ: " + value);
    }

    public static BrandStatus fromValue(Object input) {
        return fromJson(input);
    }

    @JsonCreator
    public static BrandStatus fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (BrandStatus s : values()) {
            if (s.name().equalsIgnoreCase(str) || String.valueOf(s.value).equals(str)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Trạng thái thương hiệu không hợp lệ: " + str);
    }
}
