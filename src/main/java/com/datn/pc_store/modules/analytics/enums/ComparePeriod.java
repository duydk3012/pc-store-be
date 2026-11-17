package com.datn.pc_store.modules.analytics.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kỳ đối chiếu: NONE (Không đối chiếu), PREVIOUS_PERIOD (Kỳ trước liền kề), SAME_PERIOD_LAST_YEAR (Cùng kỳ năm trước)")
public enum ComparePeriod {
    NONE,
    PREVIOUS_PERIOD,
    SAME_PERIOD_LAST_YEAR
}
