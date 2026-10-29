package com.datn.pc_store.modules.goodsreceipt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin dòng đặt hàng dùng cho việc chọn mặt hàng khi tạo phiếu nhập")
public class PurchaseOrderItemReceiptInfo {
    @Schema(description = "ID dòng đặt hàng trong đơn nhập (purchaseOrderItemId)", example = "1")
    private Long id;

    @Schema(description = "ID sản phẩm", example = "10")
    private Long productId;

    @Schema(description = "Mã sản phẩm", example = "CPU-I9-14900K")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "Intel Core i9-14900K")
    private String productName;

    @Schema(description = "Số lượng đặt mua trong đơn", example = "10")
    private int quantityOrder;

    @Schema(description = "Số lượng đã thực nhận từ các phiếu nhập trước", example = "4")
    private int quantityReceived;

    @Schema(description = "Số lượng còn lại cần nhập", example = "6")
    private int remainingQuantity;

    @Schema(description = "Đơn giá đặt mua trên đơn PO (VND), dùng gợi ý giá thực tế", example = "14000000")
    private long unitCost;
}
