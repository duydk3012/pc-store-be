package com.datn.pc_store.modules.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "7 Thẻ KPI Tổng quan hoạt động kinh doanh")
public class DashboardKpiResponse {

    @Schema(description = "Tổng doanh thu thuần hóa đơn (VND)", example = "2450000000")
    private long totalRevenue;

    @Schema(description = "Tổng số hóa đơn đã thanh toán", example = "320")
    private long totalPaidInvoices;

    @Schema(description = "Tổng số lượng sản phẩm bán ra", example = "415")
    private long totalProductsSold;

    @Schema(description = "Lợi nhuận gộp (Doanh thu - COGS) (VND)", example = "588000000")
    private long grossProfit;

    @Schema(description = "Biên lợi nhuận gộp (%)", example = "24.0")
    private double grossMarginPercentage;

    @Schema(description = "Tổng giá trị vốn tồn kho hiện tại (VND)", example = "1250000000")
    private long totalInventoryValue;

    @Schema(description = "Số mã SKU sắp hết hàng (tồn <= ngưỡng tối thiểu)", example = "8")
    private long lowStockProductCount;

    @Schema(description = "Số đơn hàng đang xử lý (chưa kết thúc)", example = "15")
    private long processingOrderCount;

    @Schema(description = "Thời điểm tính toán số liệu")
    private Instant asOfDate;
}
