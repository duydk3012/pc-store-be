package com.datn.pc_store.modules.user.dto;

import com.datn.pc_store.modules.user.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Số lượng người dùng theo vai trò")
public class UserRoleCountResponse {

    @Schema(description = "Vai trò tài khoản", example = "CUSTOMER")
    private RoleType role;

    @Schema(description = "Tên hiển thị vai trò tiếng Việt", example = "Khách hàng")
    private String label;

    @Schema(description = "Số lượng người dùng thuộc vai trò này", example = "10")
    private long count;
}
