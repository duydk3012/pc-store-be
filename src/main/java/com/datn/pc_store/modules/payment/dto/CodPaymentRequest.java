package com.datn.pc_store.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu đăng ký thanh toán khi nhận hàng (COD) cho đơn hàng")
public class CodPaymentRequest {

    @NotNull(message = "ID đơn hàng không được để trống")
    @Schema(description = "ID đơn hàng cần đăng ký thanh toán COD", example = "10001")
    private Long orderId;
}
