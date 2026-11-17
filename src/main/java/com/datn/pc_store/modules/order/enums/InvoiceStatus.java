package com.datn.pc_store.modules.order.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái hóa đơn: PENDING (0 - Chờ thanh toán), CANCELED (1 - Đã hủy), PAID (2 - Đã thanh toán)")
public enum InvoiceStatus {
    PENDING(0),
    CANCELED(1),
    PAID(2);

    private final int value;

    InvoiceStatus(int value) {
        this.value = value;
    }

    public static InvoiceStatus fromValue(int value) {
        for (var status : values()) {
            if (status.value == value) return status;
        }
        throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ: " + value);
    }

    @JsonCreator
    public static InvoiceStatus fromJson(Object input) {
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
        throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ: " + str);
    }
}
