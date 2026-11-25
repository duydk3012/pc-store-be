package com.datn.pc_store.modules.cart.dto;

import com.datn.pc_store.modules.cart.enums.CartItemAvailability;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chi tiết một dòng sản phẩm trong giỏ hàng kèm trạng thái tồn kho thời gian thực")
public class CartItemResponse {

    @Schema(description = "ID dòng giỏ hàng", example = "10001")
    private Long id;

    @Schema(description = "ID sản phẩm", example = "10003")
    private Long productId;

    @Schema(description = "Mã sản phẩm", example = "DEMO-SP-003")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "RAM Kingston 16GB DDR4 Demo")
    private String productName;

    @Schema(description = "Slug sản phẩm", example = "demo-ram-kingston-16gb")
    private String productSlug;

    @Schema(description = "Ảnh đại diện sản phẩm")
    private String productImageUrl;

    @Schema(description = "Loại sản phẩm", example = "OTHER")
    private ProductType productType;

    @Schema(description = "Trạng thái kinh doanh của sản phẩm", example = "ACTIVE")
    private ProductStatus productStatus;

    @Schema(description = "Số lượng đã chọn trong giỏ", example = "2")
    private Integer quantity;

    @Schema(description = "Đơn giá đã lưu tại thời điểm thêm/cập nhật giỏ", example = "720000")
    private Long savedUnitPrice;

    @Schema(description = "Đơn giá niêm yết hiện tại của sản phẩm", example = "720000")
    private Long currentUnitPrice;

    @Schema(description = "Cờ báo giá bán hiện tại đã thay đổi so với lúc thêm vào giỏ", example = "false")
    private boolean priceChanged;

    @Schema(description = "Tạm tính theo đơn giá hiện tại (currentUnitPrice * quantity)", example = "1440000")
    private Long subtotal;

    @Schema(description = "Số lượng tồn kho khả dụng hiện tại", example = "5")
    private Integer availableStock;

    @Schema(description = "Trạng thái khả dụng trong giỏ: AVAILABLE, INSUFFICIENT_STOCK, SOLD_OUT, UNAVAILABLE", example = "AVAILABLE")
    private CartItemAvailability availabilityStatus;

    @Schema(description = "Dòng sản phẩm có đủ điều kiện để thanh toán ngay hay không", example = "true")
    private boolean checkoutEligible;

    @Schema(description = "Thông báo chi tiết khi sản phẩm hết hàng hoặc thiếu tồn kho")
    private String availabilityMessage;

    @Schema(description = "Thời điểm thêm vào giỏ")
    private Instant createdDate;

    @Schema(description = "Thời điểm cập nhật gần nhất")
    private Instant lastModifiedDate;
}
