package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cơ cấu theo thương hiệu")
public class BrandBreakdownDto {

    @Schema(description = "ID thương hiệu", example = "1")
    private Long brandId;

    @Schema(description = "Tên thương hiệu", example = "ASUS")
    private String brandName;

    @Schema(description = "Doanh thu (VND)", example = "450000000")
    private long revenue;

    @Schema(description = "Tỷ trọng đóng góp (%)", example = "30.0")
    private double percentage;
}
