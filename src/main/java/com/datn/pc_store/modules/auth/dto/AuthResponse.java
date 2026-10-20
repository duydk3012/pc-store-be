package com.datn.pc_store.modules.auth.dto;

import com.datn.pc_store.modules.auth.enums.RoleType;

public record AuthResponse(String accessToken, String refreshToken, String tokenType,
                           long expiresInMs, String username, String fullName, RoleType role) {
}
