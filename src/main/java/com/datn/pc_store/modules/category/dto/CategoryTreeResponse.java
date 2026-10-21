package com.datn.pc_store.modules.category.dto;

import com.datn.pc_store.modules.category.enums.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin cây danh mục đa cấp")
public class CategoryTreeResponse {

    @Schema(description = "ID danh mục", example = "1")
    private Long id;

    @Schema(description = "Tên danh mục", example = "Linh kiện máy tính")
    private String name;

    @Schema(description = "Slug đường dẫn", example = "linh-kien-may-tinh")
    private String slug;

    @Schema(description = "Mô tả danh mục")
    private String description;

    @Schema(description = "Trạng thái danh mục", example = "ACTIVE")
    private CategoryStatus status;

    @Schema(description = "ID danh mục cha", example = "null")
    private Long parentId;

    @Schema(description = "Danh sách danh mục con cấp dưới")
    @Builder.Default
    private List<CategoryTreeResponse> children = new ArrayList<>();
}
