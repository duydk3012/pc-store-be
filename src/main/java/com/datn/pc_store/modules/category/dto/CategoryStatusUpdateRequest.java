package com.datn.pc_store.modules.category.dto;

import com.datn.pc_store.modules.category.enums.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật trạng thái danh mục")
public class CategoryStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái mới của danh mục: INACTIVE (0), ACTIVE (1)", example = "ACTIVE")
    private CategoryStatus status;
}
