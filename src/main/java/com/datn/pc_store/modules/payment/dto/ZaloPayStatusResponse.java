package com.datn.pc_store.modules.payment.dto;

import com.datn.pc_store.modules.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Kết quả kiểm tra và đồng bộ trạng thái giao dịch ZaloPay")
public class ZaloPayStatusResponse {

    @Schema(description = "ID bản ghi thanh toán trong hệ thống")
    private Long paymentId;

    @Schema(description = "ID đơn hàng")
    private Long orderId;

    @Schema(description = "Mã đơn hàng")
    private String orderCode;

    @Schema(description = "Mã giao dịch ứng dụng (app_trans_id)", example = "261123_10001_583920")
    private String appTransId;

    @Schema(description = "Mã giao dịch trên hệ thống ZaloPay (zp_trans_id)")
    private Long zpTransId;

    @Schema(description = "Số tiền thanh toán (VND)")
    private Long amount;

    @Schema(description = "Trạng thái bản ghi thanh toán trong DB (PENDING, SUCCESS, FAILED)")
    private PaymentStatus paymentStatus;

    @Schema(description = "Mã kết quả từ ZaloPay (1 = Thành công, 2 = Thất bại, 3 = Chưa thanh toán / Đang xử lý)")
    private Integer returnCode;

    @Schema(description = "Thông báo từ ZaloPay")
    private String returnMessage;

    @Schema(description = "Giao dịch có đang trong quá trình xử lý không")
    private boolean processing;

    @Schema(description = "Chữ ký Redirect Checksum hợp lệ (khi gọi từ Return URL)")
    private Boolean checksumValid;

    @Schema(description = "Đơn hàng đã được thanh toán thành công hay chưa")
    private boolean paid;

    @Schema(description = "Thời điểm thanh toán thành công")
    private Instant paidAt;
}
