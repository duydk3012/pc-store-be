package com.datn.pc_store.modules.goodsreceipt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Mặt hàng thực nhận theo phiếu nhập")
public class ReceiptItemRequest {
    @NotNull(message = "purchaseOrderItemId không được để trống")
    @Schema(description = "ID dòng đặt hàng trong đơn nhập purchase order", example = "1")
    private Long purchaseOrderItemId;

    @NotNull(message = "Đơn giá thực tế actualUnitCost không được để trống")
    @Min(value = 0, message = "Đơn giá thực tế không được âm")
    @Schema(description = "Giá nhập thực tế tại thời điểm nhận hàng (VND)", example = "4500000")
    private Long actualUnitCost;

    @Valid
    @Schema(description = "Danh sách serial thực nhận. Số lượng nhận, tốt và lỗi sẽ được hệ thống tự tính từ danh sách này")
    private List<ProductSerialRequest> productSerials;
}
