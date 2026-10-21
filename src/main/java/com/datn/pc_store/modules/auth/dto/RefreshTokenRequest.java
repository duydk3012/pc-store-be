package com.datn.pc_store.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yêu cầu làm mới token hoặc đăng xuất thiết bị hiện tại")
public record RefreshTokenRequest(
        @NotBlank
        @Schema(description = "Refresh token nhận được khi đăng ký, đăng nhập hoặc làm mới token gần nhất", example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2Vu")
        String refreshToken
) {
}
