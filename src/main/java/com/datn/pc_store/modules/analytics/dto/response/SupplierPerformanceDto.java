package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Hiệu suất và chất lượng giao hàng của Nhà cung cấp")
public class SupplierPerformanceDto {

    @Schema(description = "ID nhà cung cấp", example = "1")
    private Long supplierId;

    @Schema(description = "Tên nhà cung cấp", example = "Công ty CP Công Nghệ Vĩnh Xuân")
    private String supplierName;

    @Schema(description = "Mã nhà cung cấp", example = "NCC-VINHXUAN")
    private String supplierCode;

    @Schema(description = "Tổng giá trị nhập (VND)", example = "750000000")
    private long totalImportValue;

    @Schema(description = "Số lượng phiếu nhập", example = "5")
    private long receiptCount;

    @Schema(description = "Tổng số lượng hàng nhận", example = "350")
    private long totalQuantityReceived;

    @Schema(description = "Số lượng đạt", example = "345")
    private long acceptedQuantity;

    @Schema(description = "Số lượng lỗi", example = "5")
    private long defectiveQuantity;

    @Schema(description = "Tỷ lệ lỗi (%)", example = "1.43")
    private double defectRatePercentage;
}
