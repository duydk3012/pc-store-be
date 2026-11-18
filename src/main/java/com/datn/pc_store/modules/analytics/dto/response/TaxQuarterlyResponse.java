package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bảng phân bổ nghĩa vụ thuế 4 Quý trong năm")
public class TaxQuarterlyResponse {

    @Schema(description = "Năm tính thuế", example = "2026")
    private int taxYear;

    @Schema(description = "Tổng doanh thu kế toán cả năm (VND)", example = "4250000000")
    private long annualNetRevenue;

    @Schema(description = "Tổng tỷ lệ thuế trực tiếp áp dụng", example = "0.25")
    private double appliedTaxRate;

    @Builder.Default
    @Schema(description = "Chi tiết 4 Quý trong năm")
    private List<TaxQuarterDto> quarters = new ArrayList<>();
}
