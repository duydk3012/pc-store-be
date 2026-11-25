package com.datn.pc_store.modules.systemlog.enums;

public enum SystemLogAction {
    LOGIN(0, "Đăng nhập"),
    LOGOUT(1, "Đăng xuất"),
    CREATE(2, "Tạo mới"),
    UPDATE(3, "Cập nhật"),
    UPDATE_STATUS(4, "Chuyển trạng thái"),
    DELETE(5, "Xóa"),
    ASSIGN_SERIAL(6, "Gán / Tháo Serial"),
    PAYMENT_CONFIRM(7, "Xác nhận thanh toán"),
    PAYMENT_WEBHOOK(8, "Callback thanh toán"),
    EXPORT(9, "Xuất dữ liệu"),
    SCHEDULED_JOB(10, "Tác vụ định kỳ");

    private final int value;
    private final String label;

    SystemLogAction(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static SystemLogAction fromValue(int value) {
        for (SystemLogAction action : values()) {
            if (action.value == value) {
                return action;
            }
        }
        throw new IllegalArgumentException("Invalid SystemLogAction database code: " + value);
    }
}
