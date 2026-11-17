package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Phân bổ đơn hàng theo trạng thái")
public class OrderStatusDistributionDto {

    @Schema(description = "Mã trạng thái", example = "COMPLETED")
    private String status;

    @Schema(description = "Nhãn trạng thái tiếng Việt", example = "Hoàn tất giao dịch")
    private String label;

    @Schema(description = "Số lượng đơn hàng", example = "75")
    private long count;

    @Schema(description = "Tỷ lệ phần trăm (%)", example = "75.0")
    private double percentage;
}
