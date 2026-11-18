package com.datn.pc_store.modules.analytics.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bộ lọc báo cáo tồn kho")
public class InventoryFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Ngày chốt tồn kho snapshot (mặc định: hôm nay)", example = "2026-11-17")
    private LocalDate asOfDate;

    @Schema(description = "Lọc theo danh mục sản phẩm (tùy chọn)", example = "1")
    private Long categoryId;

    @Schema(description = "Lọc theo thương hiệu (tùy chọn)", example = "2")
    private Long brandId;

    @Schema(description = "Lọc theo nhà cung cấp (tùy chọn)", example = "3")
    private Long supplierId;

    @Builder.Default
    @Schema(description = "Ngưỡng cảnh báo tồn tối thiểu", example = "5")
    private Integer minThreshold = 5;
}
