package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Số liệu thuế theo Quý")
public class TaxQuarterDto {

    @Schema(description = "Quý (1 - 4)", example = "1")
    private int quarter;

    @Schema(description = "Doanh thu trong quý (VND)", example = "950000000")
    private long quarterRevenue;

    @Schema(description = "Thuế GTGT trong quý (VND)", example = "76000000")
    private long vatAmount;

    @Schema(description = "Thuế TNCN trong quý (VND)", example = "161500000")
    private long pitAmount;

    @Schema(description = "Tổng thuế trong quý (VND)", example = "237500000")
    private long totalTax;

    @Schema(description = "Số lượng hóa đơn trong quý", example = "310")
    private long invoiceCount;
}
