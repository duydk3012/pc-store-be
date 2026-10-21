package com.datn.pc_store.modules.suppliers.dto;

import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu chuyển đổi trạng thái nhà cung cấp")
public class SupplierStatusUpdateRequest {

    @NotNull(message = "Trạng thái nhà cung cấp không được để trống")
    @Schema(description = "Trạng thái mới của nhà cung cấp", example = "INACTIVE")
    private SupplierStatus status;
}
