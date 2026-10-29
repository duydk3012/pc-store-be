package com.datn.pc_store.modules.brand.dto;

import com.datn.pc_store.modules.brand.enums.BrandStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu thêm mới thương hiệu")
public class BrandCreateRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(max = 100, message = "Tên thương hiệu tối đa 100 ký tự")
    @Schema(description = "Tên thương hiệu", example = "ASUS")
    private String name;

    @Size(max = 100, message = "Slug tối đa 100 ký tự")
    @Schema(description = "Slug đường dẫn (nếu để trống sẽ tự động tạo từ tên)", example = "asus")
    private String slug;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả thương hiệu", example = "Tập đoàn công nghệ đa quốc gia sản xuất phần cứng máy tính và điện tử hàng đầu")
    private String description;

    @Schema(description = "Trạng thái thương hiệu: INACTIVE (0), ACTIVE (1); mặc định ACTIVE nếu không nhập", example = "ACTIVE")
    private BrandStatus status;
}
