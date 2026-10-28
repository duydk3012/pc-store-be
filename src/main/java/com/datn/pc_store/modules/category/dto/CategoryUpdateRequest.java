package com.datn.pc_store.modules.category.dto;

import com.datn.pc_store.modules.category.enums.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật thông tin danh mục")
public class CategoryUpdateRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    @Schema(description = "Tên danh mục", example = "Bo mạch chủ (Mainboard)")
    private String name;

    @Size(max = 100, message = "Slug tối đa 100 ký tự")
    @Schema(description = "Slug đường dẫn (nếu để trống sẽ tự động cập nhật từ tên)", example = "bo-mach-chu-mainboard")
    private String slug;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả danh mục", example = "Các dòng bo mạch chủ Intel, AMD chính hãng")
    private String description;

    @Schema(description = "Trạng thái danh mục: INACTIVE (0), ACTIVE (1)", example = "ACTIVE")
    private CategoryStatus status;

    @Schema(description = "ID của danh mục cha (null nếu muốn đưa về danh mục gốc)", example = "1")
    private Long parentId;
}
