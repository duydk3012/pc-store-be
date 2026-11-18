package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tổng quan số lượng và giá trị tồn kho")
public class InventorySummaryDto {

    @Schema(description = "Tổng số lượng serial trong kho", example = "1250")
    private long totalSerialsInStore;

    @Schema(description = "Tổng giá trị vốn tồn kho (VND)", example = "3450000000")
    private long totalInventoryValue;

    @Schema(description = "Số lượng serial có thể bán", example = "1120")
    private long sellableSerials;

    @Schema(description = "Giá trị vốn hàng có thể bán (VND)", example = "3100000000")
    private long sellableValue;

    @Schema(description = "Số lượng serial lỗi / chờ trả NCC", example = "35")
    private long defectiveSerials;

    @Schema(description = "Số lượng serial đang đóng gói / giao hàng", example = "65")
    private long packingAndDeliveringSerials;

    @Schema(description = "Số lượng serial kỹ thuật đang sử dụng", example = "30")
    private long technicalUsageSerials;
}
