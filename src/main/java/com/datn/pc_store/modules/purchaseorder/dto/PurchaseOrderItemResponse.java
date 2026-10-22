package com.datn.pc_store.modules.purchaseorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chi tiết mặt hàng trong đơn nhập")
public class PurchaseOrderItemResponse {
    @Schema(description = "ID dòng hàng, dùng để liên kết phiếu nhập thực tế", example = "1")
    private Long id;
    @Schema(description = "ID sản phẩm", example = "1")
    private Long productId;
    @Schema(description = "Số lượng đặt mua", example = "10")
    private int quantityOrder;
    @Schema(description = "Đơn giá nhập, VND", example = "5000000")
    private long unitCost;
    @Schema(description = "Số lượng đã thực nhận; chỉ nghiệp vụ xác nhận phiếu nhập được cập nhật", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    private int quantityReceived;
    @Schema(description = "Thành tiền do hệ thống tính bằng quantityOrder × unitCost, VND", example = "50000000", accessMode = Schema.AccessMode.READ_ONLY)
    private long subtotal;
}
