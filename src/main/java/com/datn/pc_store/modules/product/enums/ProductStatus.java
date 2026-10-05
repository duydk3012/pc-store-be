package com.datn.pc_store.product.enums;

public enum ProductStatus {
    ON_SALE(0, "Đang bán"),
    DISCONTINUED(1, "Đã ngưng bán"),
    OUT_OF_STOCK(2, "Đã hết hàng");

    private final int code;
    private final String label;

    ProductStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ProductStatus fromCode(int code) {
        for (ProductStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown product status code: " + code);
    }
}
