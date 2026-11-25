package com.datn.pc_store.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu tạo đơn thanh toán ZaloPay cho đơn hàng")
public class ZaloPayCreateRequest {

    @NotNull(message = "ID đơn hàng không được để trống")
    @Schema(description = "ID đơn hàng cần thanh toán qua ZaloPay", example = "10001")
    private Long orderId;

    @Schema(description = "Mã kênh thanh toán trên ZaloPay (để trống để hiển thị tất cả phương thức QR/Thẻ/Ví, hoặc truyền 'zalopayapp', 'CC', 'ATM')", example = "")
    private String bankCode;

    @Schema(description = "URL điều hướng người dùng về Frontend sau khi thanh toán (nếu bỏ trống sẽ lấy mặc định trong cấu hình)", example = "http://localhost:5173/payment/zalopay-return")
    private String redirectUrl;
}
