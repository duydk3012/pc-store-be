package com.datn.pc_store.modules.brand.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BrandStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    BrandStatus(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static BrandStatus fromValue(Object input) {
        if (input == null) {
            return ACTIVE;
        }
        if (input instanceof Number number) {
            int v = number.intValue();
            for (BrandStatus s : values()) {
                if (s.value == v) {
                    return s;
                }
            }
        }
        String str = input.toString().trim();
        for (BrandStatus s : values()) {
            if (s.name().equalsIgnoreCase(str) || String.valueOf(s.value).equals(str)) {
                return s;
            }
        }
        return ACTIVE;
    }
}
