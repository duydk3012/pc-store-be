package com.datn.pc_store.modules.category.dto;

import com.datn.pc_store.modules.category.enums.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin phản hồi của danh mục")
public class CategoryResponse {

    @Schema(description = "ID danh mục", example = "1")
    private Long id;

    @Schema(description = "Tên danh mục", example = "Linh kiện máy tính")
    private String name;

    @Schema(description = "Slug đường dẫn", example = "linh-kien-may-tinh")
    private String slug;

    @Schema(description = "Mô tả danh mục", example = "Các loại linh kiện phần cứng")
    private String description;

    @Schema(description = "Trạng thái danh mục", example = "ACTIVE")
    private CategoryStatus status;

    @Schema(description = "ID danh mục cha", example = "1")
    private Long parentId;

    @Schema(description = "Tên danh mục cha", example = "Linh kiện máy tính")
    private String parentName;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật gần nhất")
    private Long lastModifiedBy;
}
