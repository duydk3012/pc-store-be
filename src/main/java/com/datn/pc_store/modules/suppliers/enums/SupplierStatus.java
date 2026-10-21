package com.datn.pc_store.modules.suppliers.enums;

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
}
