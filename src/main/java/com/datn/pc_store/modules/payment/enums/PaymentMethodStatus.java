package com.datn.pc_store.modules.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái phương thức thanh toán: INACTIVE (0 - Ngưng hoạt động), ACTIVE (1 - Đang hoạt động)")
public enum PaymentMethodStatus {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    PaymentMethodStatus(int value) {
        this.value = value;
    }

    public static PaymentMethodStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái phương thức thanh toán không hợp lệ: " + value);
    }

    @JsonCreator
    public static PaymentMethodStatus fromJson(Object input) {
        if (input == null) return null;
        if (input instanceof Number number) {
            return fromValue(number.intValue());
        }
        String str = input.toString().trim();
        for (var status : values()) {
            if (status.name().equalsIgnoreCase(str) || String.valueOf(status.value).equals(str)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái phương thức thanh toán không hợp lệ: " + str);
    }
}
