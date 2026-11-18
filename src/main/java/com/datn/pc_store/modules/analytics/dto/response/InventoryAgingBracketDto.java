package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thống kê tháp tuổi tồn kho (Aging)")
public class InventoryAgingBracketDto {

    @Schema(description = "Mã khoảng tuổi", example = "DAYS_0_TO_30")
    private String bracket;

    @Schema(description = "Nhãn khoảng tuổi", example = "0 - 30 ngày (Mới nhập)")
    private String label;

    @Schema(description = "Số lượng serial tồn", example = "780")
    private long quantity;

    @Schema(description = "Tổng giá trị vốn tồn (VND)", example = "2150000000")
    private long totalValue;

    @Schema(description = "Tỷ lệ phần trăm (%)", example = "62.4")
    private double percentage;
}
