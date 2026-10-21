package com.datn.pc_store.modules.purchaseorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Mặt hàng đặt mua; không nhận số lượng thực nhận hay thành tiền từ client")
public class PurchaseOrderItemRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    @Positive(message = "ID sản phẩm phải lớn hơn 0")
    @Schema(description = "ID sản phẩm chưa bị xóa và không ở trạng thái INACTIVE; không được lặp trong cùng đơn", example = "1")
    private Long productId;

    @NotNull(message = "Số lượng đặt không được để trống")
    @Positive(message = "Số lượng đặt phải lớn hơn 0")
    @Schema(description = "Số lượng đặt mua", example = "10")
    private Integer quantityOrder;

    @NotNull(message = "Đơn giá nhập không được để trống")
    @PositiveOrZero(message = "Đơn giá nhập không được âm")
    @Schema(description = "Đơn giá thỏa thuận, số nguyên VND, không âm", example = "5000000")
    private Long unitCost;
}
