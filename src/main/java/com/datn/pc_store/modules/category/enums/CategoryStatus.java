package com.datn.pc_store.modules.category.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CategoryStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    CategoryStatus(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static CategoryStatus fromValue(Object input) {
        if (input == null) {
            return ACTIVE;
        }
        if (input instanceof Number number) {
            int v = number.intValue();
            for (CategoryStatus s : values()) {
                if (s.value == v) {
                    return s;
                }
            }
        }
        String str = input.toString().trim();
        for (CategoryStatus s : values()) {
            if (s.name().equalsIgnoreCase(str) || String.valueOf(s.value).equals(str)) {
                return s;
            }
        }
        return ACTIVE;
    }
}
