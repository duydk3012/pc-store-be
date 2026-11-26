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
@Schema(description = "Thông tin chi tiết bản ghi thanh toán")
public class PaymentResponse {

    @Schema(description = "ID bản ghi thanh toán", example = "10001")
    private Long id;

    @Schema(description = "ID đơn hàng", example = "10001")
    private Long orderId;

    @Schema(description = "Mã đơn hàng", example = "DH-20261123-001")
    private String orderCode;

    @Schema(description = "ID phương thức thanh toán", example = "10001")
    private Long paymentMethodId;

    @Schema(description = "Mã phương thức thanh toán (COD, ZALOPAY)", example = "COD")
    private String paymentMethodCode;

    @Schema(description = "Tên phương thức thanh toán", example = "Thanh toán khi nhận hàng (COD)")
    private String paymentMethodName;

    @Schema(description = "Số tiền thanh toán (VND)", example = "4790000")
    private Long amount;

    @Schema(description = "Mã giao dịch tham chiếu (ví dụ: COD-... hoặc app_trans_id của ZaloPay)", example = "261123_10001_583920")
    private String transactionCode;

    @Schema(description = "Trạng thái thanh toán: PENDING (0), SUCCESS (1), FAILED (2)", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "Thời điểm thanh toán thành công")
    private Instant paidAt;

    @Schema(description = "Thời điểm tạo bản ghi")
    private Instant createdDate;

    @Schema(description = "Thời điểm cập nhật cuối")
    private Instant lastModifiedDate;
}
