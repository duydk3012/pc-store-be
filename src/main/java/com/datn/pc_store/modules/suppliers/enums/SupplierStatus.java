package com.datn.pc_store.modules.suppliers.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái nhà cung cấp: INACTIVE (0), ACTIVE (1)")
public enum SupplierStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    SupplierStatus(int value) {
        this.value = value;
    }

    public static SupplierStatus fromValue(int value) {
        for (SupplierStatus s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ: " + value);
    }

    @JsonCreator
    public static SupplierStatus fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (SupplierStatus s : values()) {
            if (s.name().equalsIgnoreCase(str) || String.valueOf(s.value).equals(str)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ: " + str);
    }
}
