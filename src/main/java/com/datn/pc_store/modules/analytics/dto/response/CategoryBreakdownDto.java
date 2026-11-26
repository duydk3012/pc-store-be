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
@Schema(description = "Cơ cấu theo danh mục")
public class CategoryBreakdownDto {

    @Schema(description = "ID danh mục", example = "1")
    private Long categoryId;

    @Schema(description = "Tên danh mục", example = "VGA - Card màn hình")
    private String categoryName;

    @Schema(description = "Giá trị thống kê (Doanh thu hoặc Số lượng tồn hoặc Lợi nhuận)", example = "450000000")
    private long value;

    @Schema(description = "Tỷ trọng đóng góp (%)", example = "35.5")
    private double percentage;

    @Schema(description = "Danh sách cơ cấu danh mục con trực thuộc (tỷ trọng % tính trên tổng danh mục cha)")
    @Builder.Default
    private List<CategoryBreakdownDto> children = new ArrayList<>();
}
