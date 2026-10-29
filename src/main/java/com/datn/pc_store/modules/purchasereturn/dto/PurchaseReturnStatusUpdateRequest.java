package com.datn.pc_store.modules.purchasereturn.dto;

import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu chuyển đổi trạng thái phiếu hoàn trả")
public class PurchaseReturnStatusUpdateRequest {

    @NotNull(message = "Trạng thái mục tiêu không được để trống")
    @Schema(description = "Trạng thái mục tiêu cần chuyển đến", example = "DELIVERING")
    private PurchaseReturnStatus status;

    @Schema(description = "Ghi chú khi chuyển trạng thái (ví dụ mã vận đơn, lý do hủy)", example = "Đã bàn giao đơn vị vận chuyển Viettel Post")
    private String note;
}
