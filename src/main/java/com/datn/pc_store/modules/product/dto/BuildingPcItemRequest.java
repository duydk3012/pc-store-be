package com.datn.pc_store.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin linh kiện trong bộ cấu hình PC")
public class BuildingPcItemRequest {
    @NotNull(message = "ID linh kiện không được để trống")
    @Positive(message = "ID linh kiện phải lớn hơn 0")
    @Schema(description = "ID của sản phẩm linh kiện", example = "10001")
    private Long productId;

    @NotNull(message = "Số lượng linh kiện không được để trống")
    @Min(value = 1, message = "Số lượng linh kiện phải lớn hơn hoặc bằng 1")
    @Schema(description = "Số lượng linh kiện", example = "1")
    private Integer quantity;
}
