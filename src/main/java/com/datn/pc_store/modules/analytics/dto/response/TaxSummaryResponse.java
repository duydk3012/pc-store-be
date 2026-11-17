package com.datn.pc_store.modules.analytics.dto.response;

import com.datn.pc_store.modules.analytics.enums.TaxTierBracket;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Kết quả tính toán hoạch toán thuế")
public class TaxSummaryResponse {

    @Schema(description = "Năm tính thuế", example = "2026")
    private int taxYear;

    @Schema(description = "Quý tính thuế (null nếu tính cả năm)", example = "1")
    private Integer quarter;

    @Schema(description = "Tổng doanh thu kế toán chịu thuế trong kỳ (VND)", example = "4250000000")
    private long annualNetRevenue;

    @Schema(description = "Khung bậc thuế áp dụng")
    private TaxTierBracket appliedTier;

    @Schema(description = "Mô tả khung bậc thuế", example = "Doanh thu từ trên 3 tỷ đến 50 tỷ trên năm")
    private String tierDescription;

    @Schema(description = "Thuế suất GTGT trực tiếp", example = "0.08")
    private double vatRate;

    @Schema(description = "Số tiền thuế GTGT phải nộp (VND)", example = "340000000")
    private long vatAmount;

    @Schema(description = "Thuế suất TNCN trực tiếp", example = "0.17")
    private double pitRate;

    @Schema(description = "Số tiền thuế TNCN phải nộp (VND)", example = "722500000")
    private long pitAmount;

    @Schema(description = "Tổng tỷ lệ thuế trực tiếp trên doanh thu", example = "0.25")
    private double totalTaxRate;

    @Schema(description = "Tổng số tiền thuế phải nộp (VND)", example = "1062500000")
    private long totalTaxAmount;

    @Schema(description = "Tổng số hóa đơn hợp lệ đã phát hành", example = "1420")
    private long totalPaidInvoices;

    @Schema(description = "Thời điểm kết xuất báo cáo")
    private Instant generatedAt;
}
