package com.datn.pc_store.modules.analytics.util;

import com.datn.pc_store.common.exception.BadRequestException;

import java.time.*;
import java.time.temporal.ChronoUnit;

public final class AnalyticsDateHelper {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AnalyticsDateHelper() {}

    public static Instant[] resolveRange(LocalDate startDate, LocalDate endDate, int defaultDaysBack) {
        LocalDate today = LocalDate.now(VN_ZONE);

        LocalDate start = startDate != null ? startDate : today.minusDays(defaultDaysBack);
        LocalDate end = endDate != null ? endDate : today;

        if (start.isAfter(end)) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }

        if (end.isAfter(today)) {
            throw new BadRequestException("Không thể chọn ngày trong tương lai cho báo cáo thực tế.");
        }

        long daysBetween = ChronoUnit.DAYS.between(start, end);
        if (daysBetween > 1095) {
            throw new BadRequestException("Khoảng thời gian tối đa cho phép là 3 năm (1.095 ngày).");
        }

        Instant fromTime = start.atStartOfDay(VN_ZONE).toInstant();
        Instant toTime = end.atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        return new Instant[]{fromTime, toTime};
    }

    public static Instant[] resolveQuarterRange(int year, int quarter) {
        LocalDate start;
        LocalDate end;
        switch (quarter) {
            case 1 -> {
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 3, 31);
            }
            case 2 -> {
                start = LocalDate.of(year, 4, 1);
                end = LocalDate.of(year, 6, 30);
            }
            case 3 -> {
                start = LocalDate.of(year, 7, 1);
                end = LocalDate.of(year, 9, 30);
            }
            case 4 -> {
                start = LocalDate.of(year, 10, 1);
                end = LocalDate.of(year, 12, 31);
            }
            default -> throw new BadRequestException("Quý phải nằm trong khoảng từ 1 đến 4.");
        }

        Instant fromTime = start.atStartOfDay(VN_ZONE).toInstant();
        Instant toTime = end.atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        return new Instant[]{fromTime, toTime};
    }

    public static Instant[] resolveYearRange(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        Instant fromTime = start.atStartOfDay(VN_ZONE).toInstant();
        Instant toTime = end.atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        return new Instant[]{fromTime, toTime};
    }

    public static Instant[] resolveMonthRange(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Tháng phải nằm trong khoảng từ 1 đến 12.");
        }
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        Instant fromTime = start.atStartOfDay(VN_ZONE).toInstant();
        Instant toTime = end.atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

        return new Instant[]{fromTime, toTime};
    }
}
