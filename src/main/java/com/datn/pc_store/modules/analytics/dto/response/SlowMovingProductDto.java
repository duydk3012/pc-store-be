package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Sản phẩm bán chậm hoặc tồn đọng lâu")
public class SlowMovingProductDto {

    @Schema(description = "ID sản phẩm", example = "105")
    private Long productId;

    @Schema(description = "Mã SKU", example = "VGA-RTX4070-ASUS")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "Card màn hình ASUS Dual GeForce RTX 4070 OC 12GB")
    private String productName;

    @Schema(description = "Tên danh mục", example = "VGA - Card màn hình")
    private String categoryName;

    @Schema(description = "Số lượng có thể bán", example = "10")
    private long sellableQuantity;

    @Schema(description = "Ngày bán thành công gần nhất")
    private Instant lastSoldDate;

    @Schema(description = "Số ngày chưa phát sinh đơn bán", example = "45")
    private long daysWithoutSale;
}
