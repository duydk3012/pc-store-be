package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cơ cấu theo phương thức thanh toán")
public class PaymentMethodBreakdownDto {

    @Schema(description = "ID phương thức", example = "1")
    private Long methodId;

    @Schema(description = "Tên phương thức thanh toán", example = "Chuyển khoản ngân hàng")
    private String methodName;

    @Schema(description = "Tổng tiền thanh toán (VND)", example = "750000000")
    private long amount;

    @Schema(description = "Tỷ trọng đóng góp (%)", example = "60.0")
    private double percentage;
}
