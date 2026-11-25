package com.datn.pc_store.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Phản hồi tạo giao dịch thanh toán ZaloPay Sandbox")
public class ZaloPayCreateResponse {

    @Schema(description = "ID bản ghi thanh toán trong hệ thống")
    private Long paymentId;

    @Schema(description = "ID đơn hàng")
    private Long orderId;

    @Schema(description = "Mã đơn hàng")
    private String orderCode;

    @Schema(description = "Mã giao dịch tham chiếu ZaloPay (app_trans_id)", example = "261123_10001_583920")
    private String appTransId;

    @Schema(description = "Số tiền thanh toán (VND)", example = "4790000")
    private Long amount;

    @Schema(description = "URL chuyển hướng người dùng sang cổng thanh toán ZaloPay")
    private String orderUrl;

    @Schema(description = "Token giao dịch ZaloPay (zp_trans_token)")
    private String zpTransToken;

    @Schema(description = "Chuỗi QR Code thanh toán (nếu có)")
    private String qrCode;

    @Schema(description = "Mã kết quả trả về từ ZaloPay (1 = thành công)", example = "1")
    private Integer returnCode;

    @Schema(description = "Thông báo kết quả từ ZaloPay", example = "Giao dịch thành công")
    private String returnMessage;
}
