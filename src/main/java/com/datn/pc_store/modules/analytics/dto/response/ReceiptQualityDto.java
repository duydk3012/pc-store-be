package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chất lượng kiểm hàng nhập")
public class ReceiptQualityDto {

    @Schema(description = "Nhãn (Tên NCC hoặc Mốc thời gian)", example = "Tháng 10/2026")
    private String label;

    @Schema(description = "Số lượng đạt", example = "480")
    private long acceptedQuantity;

    @Schema(description = "Số lượng lỗi", example = "10")
    private long defectiveQuantity;

    @Schema(description = "Tổng số lượng kiểm", example = "490")
    private long totalQuantity;

    @Schema(description = "Tỷ lệ lỗi (%)", example = "2.04")
    private double defectRatePercentage;
}
