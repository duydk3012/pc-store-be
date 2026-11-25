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
@Schema(description = "Yêu cầu thêm sản phẩm vào giỏ hàng")
public class CartAddItemRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    @Schema(description = "ID sản phẩm", example = "10003")
    private Long productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng thêm vào giỏ tối thiểu là 1")
    @Schema(description = "Số lượng thêm vào giỏ hàng", example = "1")
    private Integer quantity;
}
