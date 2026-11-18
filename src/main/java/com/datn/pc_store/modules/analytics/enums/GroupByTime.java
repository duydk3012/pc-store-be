package com.datn.pc_store.modules.analytics.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nhóm thời gian: AUTO (Tự động theo khoảng ngày), HOUR (Theo giờ), DAY (Theo ngày), WEEK (Theo tuần), MONTH (Theo tháng), YEAR (Theo năm)")
public enum GroupByTime {
    AUTO,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR
}
