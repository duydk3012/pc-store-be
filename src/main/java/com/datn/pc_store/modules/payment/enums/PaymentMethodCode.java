package com.datn.pc_store.modules.payment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Mã phương thức thanh toán được hỗ trợ: COD (Thanh toán khi nhận hàng), ZALOPAY (Ví điện tử / Cổng thanh toán ZaloPay)")
public enum PaymentMethodCode {
    COD,
    ZALOPAY;

    @JsonCreator
    public static PaymentMethodCode fromJson(Object input) {
        if (input == null) return null;
        String str = input.toString().trim();
        for (var code : values()) {
            if (code.name().equalsIgnoreCase(str)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ (chỉ hỗ trợ COD hoặc ZALOPAY): " + str);
    }
}
