package com.datn.pc_store.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật số lượng sản phẩm trong giỏ hàng")
public class CartUpdateItemRequest {

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    @Schema(description = "Số lượng mới (truyền 0 để xóa sản phẩm khỏi giỏ)", example = "2")
    private Integer quantity;
}
