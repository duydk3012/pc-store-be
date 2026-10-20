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
@Schema(description = "Yêu cầu cập nhật thông tin thương hiệu")
public class BrandUpdateRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(max = 100, message = "Tên thương hiệu tối đa 100 ký tự")
    @Schema(description = "Tên thương hiệu", example = "ASUS ROG")
    private String name;

    @Size(max = 100, message = "Slug tối đa 100 ký tự")
    @Schema(description = "Slug đường dẫn", example = "asus-rog")
    private String slug;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả thương hiệu", example = "Republic of Gamers - Dòng sản phẩm chuyên game của ASUS")
    private String description;

    @Schema(description = "Trạng thái thương hiệu (ACTIVE: 1, INACTIVE: 0)", example = "ACTIVE")
    private BrandStatus status;
}
