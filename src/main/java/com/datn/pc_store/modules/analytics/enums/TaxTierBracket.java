package com.datn.pc_store.modules.analytics.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Bậc thuế suất doanh thu năm")
public enum TaxTierBracket {
    UNDER_1B("Doanh thu dưới 1 tỷ / năm", 0.0, 0.0, 0.0),
    FROM_1B_TO_3B("Doanh thu từ 1 tỷ đến 3 tỷ / năm", 0.01, 0.005, 0.015),
    FROM_3B_TO_50B("Doanh thu từ trên 3 tỷ đến 50 tỷ / năm", 0.08, 0.17, 0.25),
    OVER_50B("Doanh thu trên 50 tỷ / năm", 0.08, 0.20, 0.28);

    private final String description;
    private final double vatRate; // Thuế GTGT
    private final double pitRate; // Thuế TNCN
    private final double totalDirectRate; // Tổng tỷ lệ trực tiếp

    TaxTierBracket(String description, double vatRate, double pitRate, double totalDirectRate) {
        this.description = description;
        this.vatRate = vatRate;
        this.pitRate = pitRate;
        this.totalDirectRate = totalDirectRate;
    }

    public static TaxTierBracket determineTier(long annualRevenue) {
        if (annualRevenue < 1_000_000_000L) {
            return UNDER_1B;
        } else if (annualRevenue <= 3_000_000_000L) {
            return FROM_1B_TO_3B;
        } else if (annualRevenue <= 50_000_000_000L) {
            return FROM_3B_TO_50B;
        } else {
            return OVER_50B;
        }
    }
}
