package com.datn.pc_store.modules.goodsreceipt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chi tiết dòng nhận hàng")
public class ReceiptItemResponse {
    @Schema(description = "ID dòng nhận hàng", example = "1")
    private Long id;

    @Schema(description = "ID phiếu nhập hàng", example = "1")
    private Long goodsReceiptId;

    @Schema(description = "ID dòng đơn đặt hàng purchase order item", example = "1")
    private Long purchaseOrderItemId;

    @Schema(description = "Đơn giá thực nhận, VND", example = "4500000")
    private long actualUnitCost;

    @Schema(description = "Tổng số lượng thực nhận (tính từ tổng số serial)", example = "10")
    private int quantityReceived;

    @Schema(description = "Số lượng hàng đạt chuẩn (serial ở trạng thái GOOD_CONDITION / IN_STOCK)", example = "9")
    private int quantityAccepted;

    @Schema(description = "Số lượng hàng lỗi hỏng (serial ở trạng thái DEFECTIVE / PENDING_RETURN)", example = "1")
    private int quantityDefective;

    @Schema(description = "Danh sách chi tiết serials của dòng hàng")
    private List<ProductSerialResponse> productSerials;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật")
    private Long lastModifiedBy;
}
