package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tổng quan nhập hàng")
public class PurchaseOverviewDto {

    @Schema(description = "Tổng giá trị nhập hàng (VND)", example = "1450000000")
    private long totalImportValue;

    @Schema(description = "Tổng số phiếu nhập đã nhận", example = "12")
    private long totalReceipts;

    @Schema(description = "Tổng số lượng hàng đạt", example = "580")
    private long acceptedQuantity;

    @Schema(description = "Tổng số lượng hàng lỗi khi kiểm", example = "12")
    private long defectiveQuantity;

    @Schema(description = "Tỷ lệ hàng lỗi (%)", example = "2.03")
    private double defectRatePercentage;
}
