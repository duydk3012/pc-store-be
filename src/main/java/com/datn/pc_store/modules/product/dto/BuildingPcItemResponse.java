package com.datn.pc_store.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết linh kiện trong bộ cấu hình PC")
public class BuildingPcItemResponse {
    @Schema(description = "ID bản ghi cấu hình linh kiện", example = "1")
    private Long id;

    @Schema(description = "ID linh kiện", example = "10001")
    private Long productId;

    @Schema(description = "Tên linh kiện", example = "CPU Intel Core i5-13400")
    private String productName;

    @Schema(description = "Mã linh kiện", example = "CPU-INTEL-I5-13400")
    private String productCode;

    @Schema(description = "Đường dẫn ảnh linh kiện", example = "https://example.com/images/cpu.jpg")
    private String imageUrl;

    @Schema(description = "Giá bán hiện tại của linh kiện (VND)", example = "4800000")
    private Long price;

    @Schema(description = "Số lượng", example = "1")
    private Integer quantity;
}
