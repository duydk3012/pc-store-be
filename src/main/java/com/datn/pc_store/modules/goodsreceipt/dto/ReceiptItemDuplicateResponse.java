package com.datn.pc_store.modules.goodsreceipt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu mẫu dòng mặt hàng thực nhận khi nhân bản phiếu nhập hàng")
public class ReceiptItemDuplicateResponse {

    @Schema(description = "ID dòng đặt hàng trong đơn nhập purchaseOrderItemId", example = "1")
    private Long purchaseOrderItemId;

    @Schema(description = "ID sản phẩm", example = "10")
    private Long productId;

    @Schema(description = "Mã sản phẩm", example = "CPU-I9-14900K")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "Intel Core i9-14900K")
    private String productName;

    @Schema(description = "Đơn giá thực nhận sao chép từ phiếu cũ (VND)", example = "4500000")
    private long actualUnitCost;

    @Schema(description = "Đơn giá đặt mua trên đơn PO (VND)", example = "4500000")
    private long unitCost;

    @Schema(description = "Số lượng đặt mua trong đơn PO", example = "10")
    private int quantityOrder;

    @Schema(description = "Số lượng đã nhận trong đơn PO", example = "4")
    private int quantityReceivedInPo;

    @Schema(description = "Số lượng còn lại có thể nhận trong PO", example = "6")
    private int remainingQuantityInOrder;

    @Schema(description = "Tổng số lượng thực nhận (tính từ số serial)", example = "5")
    private int quantityReceived;

    @Schema(description = "Số lượng đạt chuẩn (GOOD_CONDITION)", example = "5")
    private int quantityAccepted;

    @Schema(description = "Số lượng lỗi hỏng (DEFECTIVE)", example = "0")
    private int quantityDefective;

    @Schema(description = "Danh sách serials mẫu đã chuẩn hóa trạng thái để tạo mới")
    private List<ProductSerialDuplicateResponse> productSerials;
}
