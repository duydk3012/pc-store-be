package com.datn.pc_store.modules.payment.dto;

import com.datn.pc_store.modules.payment.enums.PaymentMethodStatus;
import com.datn.pc_store.modules.payment.enums.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin phương thức thanh toán")
public class PaymentMethodResponse {

    @Schema(description = "ID phương thức thanh toán", example = "10001")
    private Long id;

    @Schema(description = "Mã phương thức thanh toán (COD, ZALOPAY)", example = "COD")
    private String code;

    @Schema(description = "Tên hiển thị phương thức thanh toán", example = "Thanh toán khi nhận hàng (COD)")
    private String name;

    @Schema(description = "Loại hình: CASH (0) hoặc ONLINE (1)", example = "CASH")
    private PaymentMethodType type;

    @Schema(description = "Tên ngân hàng / cổng thanh toán", example = "ZaloPay Sandbox")
    private String bankName;

    @Schema(description = "Số tài khoản / AppID tích hợp", example = "554")
    private String accountNumber;

    @Schema(description = "Trạng thái hoạt động", example = "ACTIVE")
    private PaymentMethodStatus status;
}
