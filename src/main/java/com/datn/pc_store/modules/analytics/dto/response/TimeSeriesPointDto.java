package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Điểm dữ liệu biểu đồ chuỗi thời gian")
public class TimeSeriesPointDto {

    @Schema(description = "Mốc thời gian (yyyy-MM-dd hoặc chuỗi nhóm)", example = "2026-11-01")
    private String timeBucket;

    @Schema(description = "Nhãn hiển thị trên trục hoành", example = "01/11")
    private String label;

    @Schema(description = "Doanh thu (VND)", example = "75000000")
    private long revenue;

    @Schema(description = "Giá vốn hàng bán COGS (VND)", example = "57000000")
    private long cogs;

    @Schema(description = "Lợi nhuận gộp (VND)", example = "18000000")
    private long grossProfit;

    @Schema(description = "Số lượng đơn hàng / hóa đơn", example = "10")
    private long orderCount;
}
