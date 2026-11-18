package com.datn.pc_store.modules.analytics.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bộ lọc hoạch toán thuế")
public class TaxReportFilterRequest {

    @NotNull(message = "Năm tính thuế không được để trống")
    @Min(value = 2020, message = "Năm tính thuế tối thiểu từ 2020")
    @Schema(description = "Năm tính thuế", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;

    @Min(value = 1, message = "Quý phải từ 1 đến 4")
    @Max(value = 4, message = "Quý phải từ 1 đến 4")
    @Schema(description = "Quý tính thuế (tùy chọn: 1, 2, 3, 4 hoặc để trống để tính cả năm)", example = "1")
    private Integer quarter;

    @Min(value = 1, message = "Tháng phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12")
    @Schema(description = "Tháng tính thuế (tùy chọn: 1 - 12)", example = "10")
    private Integer month;
}
