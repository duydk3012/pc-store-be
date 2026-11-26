package com.datn.pc_store.modules.warranty.dto;

import com.datn.pc_store.modules.warranty.enums.WarrantyPartType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record WarrantyRequestCreateRequest(
        @NotNull @Positive Long warrantyId,
        @NotNull @Positive Long customerId,
        @NotBlank @Size(max = 500) String issueDescription,
        List<@NotNull @Valid Part> parts
) {
    public record Part(
            @Schema(description = "ID sản phẩm (Bắt buộc với WARRANTY và UPGRADE, tùy chọn với REPAIR)")
            Long productId,

            @NotNull @Positive
            @Schema(description = "Số lượng", example = "1")
            Integer quantity,

            @Schema(description = "Loại linh kiện/dịch vụ: WARRANTY (0), REPAIR (1), UPGRADE (2). Mặc định là WARRANTY.")
            WarrantyPartType type,

            @Min(0) @Max(100)
            @Schema(description = "Phần trăm bảo hành (chỉ áp dụng cho WARRANTY: 100 = miễn phí, 50 = trả 50%). Mặc định 100")
            Integer warrantyCoveragePercent,

            @PositiveOrZero
            @Schema(description = "Đơn giá tính tiền (nếu bỏ trống hệ thống sẽ tự tính theo công thức)")
            Long unitCost,

            @Schema(description = "Danh sách ID serial gán ngay (thường để trống, nhân viên kho quét sau)")
            List<Long> productSerialIds,

            @Schema(description = "Danh sách mã serial gán ngay (thường để trống, nhân viên kho quét sau)")
            List<String> serialNumbers
    ) {}
}
