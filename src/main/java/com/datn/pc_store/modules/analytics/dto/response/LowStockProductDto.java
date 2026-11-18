package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Sản phẩm cảnh báo sắp hết hàng")
public class LowStockProductDto {

    @Schema(description = "ID sản phẩm", example = "105")
    private Long productId;

    @Schema(description = "Mã SKU", example = "VGA-RTX4070-ASUS")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "Card màn hình ASUS Dual GeForce RTX 4070 OC 12GB")
    private String productName;

    @Schema(description = "Tên danh mục", example = "VGA - Card màn hình")
    private String categoryName;

    @Schema(description = "Số lượng có thể bán hiện tại", example = "2")
    private long sellableQuantity;

    @Schema(description = "Ngưỡng tồn tối thiểu", example = "5")
    private int minimumThreshold;

    @Schema(description = "Nhà cung cấp gần nhất", example = "Công ty CP Công Nghệ Vĩnh Xuân")
    private String suggestedSupplier;
}
