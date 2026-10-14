package com.datn.pc_store.modules.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 50)
        String username,
        @NotBlank
        @Size(min = 8, max = 72)
        String password,
        @NotBlank
        @Size(max = 100)
        String fullName,
        @NotBlank
        @Email
        String email,
        @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
        String phoneNumber,
        String address) {

}
