package com.datn.pc_store.modules.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Trạng thái người dùng: INACTIVE (0), ACTIVE (1), BANNED (2)")
public enum UserStatus {
    INACTIVE(0),
    ACTIVE(1),
    BANNED(2);

    private final int value;

    UserStatus(int value) {
        this.value = value;
    }

    public static UserStatus fromValue(int value) {
        for (UserStatus s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        throw new IllegalArgumentException("Trạng thái người dùng không hợp lệ: " + value);
    }
}
