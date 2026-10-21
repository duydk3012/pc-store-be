package com.datn.pc_store.modules.product.dto;

import com.datn.pc_store.modules.product.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật trạng thái sản phẩm")
public class ProductStatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái mới: INACTIVE (ẩn), ACTIVE (hoạt động), OUT_OF_STOCK (hết hàng)", example = "OUT_OF_STOCK")
    private ProductStatus status;
}
