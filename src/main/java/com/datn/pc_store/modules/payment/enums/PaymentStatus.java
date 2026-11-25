package com.datn.pc_store.modules.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái thanh toán: PENDING (0 - Chờ thanh toán), SUCCESS (1 - Đã thanh toán thành công), FAILED (2 - Thất bại / Đã hủy)")
public enum PaymentStatus {
    PENDING(0),
    SUCCESS(1),
    FAILED(2);

    private final int value;

    PaymentStatus(int value) {
        this.value = value;
    }

    public static PaymentStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + value);
    }

    @JsonCreator
    public static PaymentStatus fromJson(Object input) {
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
        throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + str);
    }
}
