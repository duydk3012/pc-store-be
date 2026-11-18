package com.datn.pc_store.modules.analytics.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Phân loại tháp tuổi tồn kho")
public enum InventoryAgingBracket {
    DAYS_0_TO_30("0 - 30 ngày (Mới nhập)"),
    DAYS_31_TO_60("31 - 60 ngày (Bình thường)"),
    DAYS_61_TO_90("61 - 90 ngày (Cảnh báo tồn)"),
    DAYS_OVER_90("Trên 90 ngày (Tồn lâu / Đọng vốn)");

    private final String label;

    InventoryAgingBracket(String label) {
        this.label = label;
    }
}
