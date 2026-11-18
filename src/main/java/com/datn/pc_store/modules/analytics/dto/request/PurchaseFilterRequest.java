package com.datn.pc_store.modules.analytics.dto.request;

import com.datn.pc_store.modules.analytics.enums.GroupByTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bộ lọc báo cáo nhập hàng & nhà cung cấp")
public class PurchaseFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Ngày bắt đầu (yyyy-MM-dd)", example = "2026-10-01")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Ngày kết thúc (yyyy-MM-dd)", example = "2026-10-31")
    private LocalDate endDate;

    @Builder.Default
    @Schema(description = "Nhóm dữ liệu theo thời gian", example = "AUTO")
    private GroupByTime groupBy = GroupByTime.AUTO;

    @Schema(description = "Lọc theo nhà cung cấp (tùy chọn)", example = "1")
    private Long supplierId;

    @Schema(description = "Lọc theo danh mục sản phẩm (tùy chọn)", example = "2")
    private Long categoryId;
}
