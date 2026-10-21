package com.datn.pc_store.modules.brand.dto;

import com.datn.pc_store.modules.brand.enums.BrandStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin phản hồi của thương hiệu")
public class BrandResponse {

    @Schema(description = "ID thương hiệu", example = "1")
    private Long id;

    @Schema(description = "Tên thương hiệu", example = "ASUS")
    private String name;

    @Schema(description = "Slug đường dẫn", example = "asus")
    private String slug;

    @Schema(description = "Mô tả thương hiệu", example = "Tập đoàn công nghệ đa quốc gia")
    private String description;

    @Schema(description = "Trạng thái thương hiệu", example = "ACTIVE")
    private BrandStatus status;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật gần nhất")
    private Long lastModifiedBy;
}
