package com.datn.pc_store.modules.analytics.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Nhóm trạng thái tồn kho serial")
public enum InventoryStockGroup {
    SELLABLE("Có thể bán"),
    COMMITTED("Đang đóng gói / Giao hàng"),
    DEFECTIVE("Hàng lỗi / Đang trả NCC"),
    TECHNICAL("Sử dụng kỹ thuật / Linh kiện"),
    SOLD("Đã bán xuất kho");

    private final String label;

    InventoryStockGroup(String label) {
        this.label = label;
    }
}
