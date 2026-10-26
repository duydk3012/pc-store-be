package com.datn.pc_store.modules.goodsreceipt.dto;

import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu chuyển trạng thái phiếu nhập")
public class GoodsReceiptStatusUpdateRequest {
    @NotNull(message = "Trạng thái mới không được để trống")
    @Schema(description = "Trạng thái mục tiêu: DRAFT, PENDING, CONFIRMED, REJECTED, CANCELLED", example = "CONFIRMED")
    private GoodsReceiptStatus status;
}
