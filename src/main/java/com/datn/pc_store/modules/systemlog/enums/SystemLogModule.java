package com.datn.pc_store.modules.systemlog.enums;

public enum SystemLogModule {
    AUTH(0, "Xác thực & Phiên đăng nhập"),
    USER(1, "Quản lý người dùng"),
    CATALOG(2, "Danh mục & Sản phẩm"),
    SUPPLIER(3, "Nhà cung cấp"),
    PURCHASE_ORDER(4, "Đơn đặt hàng mua"),
    GOODS_RECEIPT(5, "Phiếu nhập kho"),
    PRODUCT_SERIAL(6, "Quản lý Serial"),
    PURCHASE_RETURN(7, "Hoàn trả nhà cung cấp"),
    ORDER(8, "Đơn bán hàng"),
    PAYMENT(9, "Thanh toán & Hóa đơn"),
    DELIVERY(10, "Giao vận"),
    WARRANTY(11, "Bảo hành & Kỹ thuật"),
    SYSTEM(12, "Tác vụ hệ thống");

    private final int value;
    private final String label;

    SystemLogModule(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static SystemLogModule fromValue(int value) {
        for (SystemLogModule module : values()) {
            if (module.value == value) {
                return module;
            }
        }
        throw new IllegalArgumentException("Invalid SystemLogModule database code: " + value);
    }
}
