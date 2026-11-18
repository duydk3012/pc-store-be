package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Top sản phẩm bán chạy hoặc sinh lời cao")
public class TopProductDto {

    @Schema(description = "ID sản phẩm", example = "204")
    private Long productId;

    @Schema(description = "Mã SKU", example = "CPU-I5-14600K")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "CPU Intel Core i5 14600K")
    private String productName;

    @Schema(description = "Tên danh mục", example = "CPU - Vi xử lý")
    private String categoryName;

    @Schema(description = "Số lượng bán thuần", example = "85")
    private long quantitySold;

    @Schema(description = "Doanh thu (VND)", example = "595000000")
    private long revenue;

    @Schema(description = "Giá vốn hàng bán COGS (VND)", example = "480000000")
    private long cogs;

    @Schema(description = "Lợi nhuận gộp (VND)", example = "115000000")
    private long grossProfit;

    @Schema(description = "Biên lợi nhuận gộp (%)", example = "19.33")
    private double grossMarginPercentage;
}
