package com.datn.pc_store.modules.user.enums;

public enum RoleType {
    ADMIN(0),
    MANAGER(1),
    CUSTOMER(2),
    STAFF_WAREHOUSE(3),
    STAFF_SALES(4),
    STAFF_TECH(5);

    private final int value;

    RoleType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return switch (this) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Quản lý";
            case CUSTOMER -> "Khách hàng";
            case STAFF_WAREHOUSE -> "Nhân viên kho";
            case STAFF_SALES -> "Nhân viên bán hàng";
            case STAFF_TECH -> "Nhân viên kỹ thuật";
        };
    }

    public static RoleType fromValue(int value) {
        for (RoleType role : values()) {
            if (role.value == value) return role;
        }
        throw new IllegalArgumentException("Invalid role database code: " + value);
    }
}
