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
@Schema(description = "Yêu cầu thêm mới danh mục sản phẩm")
public class CategoryCreateRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    @Schema(description = "Tên danh mục", example = "Linh kiện máy tính")
    private String name;

    @Size(max = 100, message = "Slug tối đa 100 ký tự")
    @Schema(description = "Slug đường dẫn (nếu để trống sẽ tự động tạo từ tên)", example = "linh-kien-may-tinh")
    private String slug;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả chi tiết danh mục", example = "Các loại linh kiện phần cứng máy tính để bàn và laptop")
    private String description;

    @Schema(description = "Trạng thái danh mục: INACTIVE (0), ACTIVE (1); mặc định ACTIVE nếu không nhập", example = "ACTIVE")
    private CategoryStatus status;

    @Schema(description = "ID của danh mục cha (để trống nếu là danh mục gốc cấp 1)", example = "1")
    private Long parentId;
}
