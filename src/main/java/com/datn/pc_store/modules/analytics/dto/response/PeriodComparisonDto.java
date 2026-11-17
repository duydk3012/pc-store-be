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
@Schema(description = "Đối chiếu doanh thu giữa các kỳ")
public class PeriodComparisonDto {

    @Schema(description = "Doanh thu kỳ hiện tại (VND)", example = "850000000")
    private long currentRevenue;

    @Schema(description = "Doanh thu kỳ đối chiếu (VND)", example = "720000000")
    private long comparedRevenue;

    @Schema(description = "Chênh lệch doanh thu (VND)", example = "130000000")
    private long growthAmount;

    @Schema(description = "Tốc độ tăng trưởng (%)", example = "18.06")
    private double growthRatePercentage;

    @Schema(description = "Nhãn kỳ hiện tại", example = "10/2026")
    private String currentPeriodLabel;

    @Schema(description = "Nhãn kỳ đối chiếu", example = "09/2026")
    private String comparedPeriodLabel;

    @Builder.Default
    @Schema(description = "Danh sách điểm dữ liệu so sánh chi tiết")
    private List<TimeSeriesPointDto> comparisonPoints = new ArrayList<>();
}
