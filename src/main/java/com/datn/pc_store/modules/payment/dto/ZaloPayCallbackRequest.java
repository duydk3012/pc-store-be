package com.datn.pc_store.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu Webhook Callback từ ZaloPay Server gửi về")
public class ZaloPayCallbackRequest {

    @Schema(description = "Chuỗi JSON chứa dữ liệu giao dịch từ ZaloPay")
    private String data;

    @Schema(description = "Chữ ký HMAC-SHA256 tạo bởi Key2")
    private String mac;

    @Schema(description = "Loại callback (1 = Order, 2 = Agreement)", example = "1")
    private Integer type;
}
