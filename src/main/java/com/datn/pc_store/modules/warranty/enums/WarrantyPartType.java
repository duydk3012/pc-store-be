package com.datn.pc_store.modules.warranty.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Loại linh kiện bảo hành: WARRANTY (0 - Bảo hành), REPAIR (1 - Sửa chữa), UPGRADE (2 - Nâng cấp)")
public enum WarrantyPartType {
    WARRANTY(0),
    REPAIR(1),
    UPGRADE(2);

    private final int value;

    WarrantyPartType(int value) {
        this.value = value;
    }

    public static WarrantyPartType fromValue(int value) {
        for (WarrantyPartType t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("Loại linh kiện bảo hành không hợp lệ: " + value);
    }

    @JsonCreator
    public static WarrantyPartType fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (WarrantyPartType t : values()) {
            if (t.name().equalsIgnoreCase(str) || String.valueOf(t.value).equals(str)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Loại linh kiện bảo hành không hợp lệ: " + str);
    }
}
