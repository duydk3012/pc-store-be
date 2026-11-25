package com.datn.pc_store.modules.systemlog.enums;

public enum SystemLogStatus {
    FAILED(0, "Thất bại"),
    SUCCESS(1, "Thành công");

    private final int value;
    private final String label;

    SystemLogStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static SystemLogStatus fromValue(int value) {
        for (SystemLogStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid SystemLogStatus database code: " + value);
    }
}
