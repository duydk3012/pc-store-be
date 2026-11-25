package com.datn.pc_store.modules.cart.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trạng thái khả dụng của sản phẩm trong giỏ hàng: AVAILABLE (Còn đủ hàng), INSUFFICIENT_STOCK (Không đủ số lượng), SOLD_OUT (Hết hàng), UNAVAILABLE (Ngừng kinh doanh)")
public enum CartItemAvailability {
    AVAILABLE,
    INSUFFICIENT_STOCK,
    SOLD_OUT,
    UNAVAILABLE
}
