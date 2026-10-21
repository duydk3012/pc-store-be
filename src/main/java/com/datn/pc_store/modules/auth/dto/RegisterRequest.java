package com.datn.pc_store.modules.auth.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yêu cầu đăng ký tài khoản khách hàng")
public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 50)
        @Schema(description = "Tên đăng nhập duy nhất, từ 4 đến 50 ký tự", example = "customer01")
        String username,
        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(description = "Mật khẩu từ 8 đến 72 ký tự", example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,
        @Size(max = 100)
        @Schema(description = "Họ và tên, tối đa 100 ký tự (không bắt buộc)", example = "Nguyễn Văn An")
        String fullName,
        @NotBlank
        @Email
        @Schema(description = "Địa chỉ email hợp lệ và duy nhất", example = "customer01@example.com")
        String email,
        @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
        @Schema(description = "Số điện thoại bắt đầu bằng 0 hoặc +84, theo sau là 9 chữ số (không bắt buộc)", example = "0912345678")
        String phoneNumber,
        @Schema(description = "Địa chỉ liên hệ (không bắt buộc)", example = "123 Nguyễn Trãi, Hà Nội")
        String address
) {
}
