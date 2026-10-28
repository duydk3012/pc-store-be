package com.datn.pc_store.modules.brand.dto;

import com.datn.pc_store.modules.brand.enums.BrandStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật trạng thái thương hiệu")
public class BrandStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái mới của thương hiệu: INACTIVE (0), ACTIVE (1)", example = "ACTIVE")
    private BrandStatus status;
}
