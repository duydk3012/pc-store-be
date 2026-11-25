package com.datn.pc_store.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin tổng hợp giỏ hàng của khách hàng")
public class CartResponse {

    @Schema(description = "ID giỏ hàng (null nếu khách chưa có bản ghi giỏ hàng)", example = "10001")
    private Long id;

    @Schema(description = "ID khách hàng sở hữu giỏ hàng", example = "10011")
    private Long customerId;

    @Schema(description = "Tổng số dòng sản phẩm khác nhau trong giỏ", example = "3")
    private int totalDistinctItems;

    @Schema(description = "Tổng số lượng đơn vị sản phẩm trong giỏ", example = "5")
    private int totalQuantity;

    @Schema(description = "Số dòng sản phẩm đủ điều kiện thanh toán", example = "1")
    private int eligibleDistinctItems;

    @Schema(description = "Tổng số lượng đơn vị sản phẩm đủ điều kiện thanh toán", example = "2")
    private int eligibleQuantity;

    @Schema(description = "Tổng tiền hàng tạm tính của các sản phẩm đủ điều kiện thanh toán", example = "1440000")
    private long eligibleTotalAmount;

    @Schema(description = "Cờ cho biết giỏ hàng có chứa ít nhất 1 sản phẩm hết hàng/thiếu tồn/ngừng kinh doanh", example = "true")
    private boolean hasInvalidItems;

    @Schema(description = "Danh sách các dòng sản phẩm trong giỏ hàng")
    private List<CartItemResponse> items;
}
