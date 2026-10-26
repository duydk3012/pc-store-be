package com.datn.pc_store.modules.goodsreceipt.dto;

import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin phiếu nhập hàng")
public class GoodsReceiptResponse {
    @Schema(description = "ID phiếu nhập", example = "1")
    private Long id;

    @Schema(description = "Mã phiếu nhập duy nhất", example = "GR-20261026-001")
    private String code;

    @Schema(description = "Tên phiếu nhập", example = "Phiếu nhận đợt 1")
    private String name;

    @Schema(description = "ID đơn đặt hàng", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "Thời gian nhận hàng")
    private Instant receivedAt;

    @Schema(description = "ID người nhận/xác nhận hàng")
    private Long receivedBy;

    @Schema(description = "Trạng thái phiếu nhập", example = "PENDING")
    private GoodsReceiptStatus status;

    @Schema(description = "Chi tiết các mặt hàng thực nhận")
    private List<ReceiptItemResponse> items;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật gần nhất")
    private Long lastModifiedBy;
}
