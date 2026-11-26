package com.datn.pc_store.modules.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Loại phương thức thanh toán: CASH (0 - Tiền mặt / COD), ONLINE (1 - Trực tuyến / Cổng thanh toán)")
public enum PaymentMethodType {
    CASH(0),
    ONLINE(1);

    private final int value;

    PaymentMethodType(int value) {
        this.value = value;
    }

    public static PaymentMethodType fromValue(int value) {
        for (var type : values()) {
            if (type.value == value) return type;
        }
        throw new IllegalArgumentException("Loại phương thức thanh toán không hợp lệ: " + value);
    }

    @JsonCreator
    public static PaymentMethodType fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (var type : values()) {
            if (type.name().equalsIgnoreCase(str) || String.valueOf(type.value).equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Loại phương thức thanh toán không hợp lệ: " + str);
    }
}
