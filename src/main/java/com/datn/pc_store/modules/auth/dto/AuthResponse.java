package com.datn.pc_store.modules.auth.dto;

import com.datn.pc_store.modules.user.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin xác thực trả về khi đăng ký, đăng nhập hoặc làm mới token")
public record AuthResponse(
        @Schema(description = "JWT dùng để xác thực các API yêu cầu đăng nhập", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjdXN0b21lcjAxIn0.signature")
        String accessToken,
        @Schema(description = "Token dùng để cấp cặp token mới hoặc đăng xuất thiết bị hiện tại", example = "dGhpcy1pcy1hLXNhbXBsZS1yZWZyZXNoLXRva2Vu")
        String refreshToken,
        @Schema(description = "Loại token; gửi header Authorization theo định dạng Bearer <accessToken>", example = "Bearer")
        String tokenType,
        @Schema(description = "Thời gian hiệu lực của access token tính bằng mili giây kể từ lúc cấp", example = "3600000")
        long expiresInMs,
        @Schema(description = "Tên đăng nhập", example = "customer01")
        String username,
        @Schema(description = "Họ và tên tài khoản", example = "Nguyễn Văn An")
        String fullName,
        @Schema(description = "Vai trò của tài khoản", example = "CUSTOMER")
        RoleType role
) {
}
