package com.datn.pc_store.modules.purchaseorder.dto;

import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu chuyển trạng thái theo luồng nghiệp vụ đơn nhập")
public class PurchaseOrderStatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái đích: PENDING để gửi duyệt, APPROVED để duyệt, CANCELLED để hủy; RECEIVED chỉ khi đã có phiếu nhập xác nhận và nhận đủ hàng. Không được quay về DRAFT", example = "PENDING")
    private PurchaseOrderStatus status;
}
