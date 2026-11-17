package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bảng ma trận hiệu quả chi tiết từng SKU")
public class ProductMatrixDto {

    @Schema(description = "ID sản phẩm", example = "204")
    private Long productId;

    @Schema(description = "Mã SKU", example = "CPU-I5-14600K")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "CPU Intel Core i5 14600K")
    private String productName;

    @Schema(description = "Tên danh mục", example = "CPU - Vi xử lý")
    private String categoryName;

    @Schema(description = "Tên thương hiệu", example = "Intel")
    private String brandName;

    @Schema(description = "Số lượng bán thuần", example = "85")
    private long quantitySold;

    @Schema(description = "Doanh thu thuần (VND)", example = "595000000")
    private long revenue;

    @Schema(description = "Giá vốn hàng bán COGS (VND)", example = "480000000")
    private long cogs;

    @Schema(description = "Lợi nhuận gộp (VND)", example = "115000000")
    private long grossProfit;

    @Schema(description = "Tỷ suất lợi nhuận gộp (%)", example = "19.33")
    private double grossMarginPercentage;

    @Schema(description = "Tồn kho có thể bán hiện tại", example = "25")
    private long currentStock;

    @Schema(description = "Số lượt bảo hành phát sinh", example = "2")
    private long warrantyCount;
}
