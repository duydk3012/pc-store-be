package com.datn.pc_store.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yêu cầu đăng nhập bằng tên đăng nhập và mật khẩu")
public record LoginRequest(
        @NotBlank
        @Schema(description = "Tên đăng nhập đã đăng ký", example = "customer01")
        String username,
        @NotBlank
        @Schema(description = "Mật khẩu tài khoản", example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password
) {
}
