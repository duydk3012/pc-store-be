package com.datn.pc_store.modules.user.dto;

import com.datn.pc_store.modules.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật trạng thái người dùng")
public class UserStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái tài khoản: ACTIVE (1 - hoạt động), INACTIVE (0 - ngưng hoạt động), BANNED (2 - bị cấm)", example = "INACTIVE")
    private UserStatus status;
}
