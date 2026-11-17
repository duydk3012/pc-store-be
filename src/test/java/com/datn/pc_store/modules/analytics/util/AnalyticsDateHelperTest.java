package com.datn.pc_store.modules.analytics.util;

import com.datn.pc_store.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsDateHelperTest {

    @Test
    @DisplayName("Edge Case: startDate > endDate ném BadRequestException")
    void testResolveRange_StartDateAfterEndDate() {
        LocalDate start = LocalDate.of(2026, 11, 20);
        LocalDate end = LocalDate.of(2026, 11, 10);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                AnalyticsDateHelper.resolveRange(start, end, 30)
        );
        assertEquals("Ngày bắt đầu không được lớn hơn ngày kết thúc.", ex.getMessage());
    }

    @Test
    @DisplayName("Edge Case: endDate trong tương lai ném BadRequestException")
    void testResolveRange_EndDateInFuture() {
        LocalDate start = LocalDate.now(AnalyticsDateHelper.VN_ZONE).minusDays(5);
        LocalDate futureEnd = LocalDate.now(AnalyticsDateHelper.VN_ZONE).plusDays(10);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                AnalyticsDateHelper.resolveRange(start, futureEnd, 30)
        );
        assertEquals("Không thể chọn ngày trong tương lai cho báo cáo thực tế.", ex.getMessage());
    }

    @Test
    @DisplayName("Edge Case: Khoảng cách > 3 năm (1095 ngày) ném BadRequestException")
    void testResolveRange_ExceedMaxDays() {
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                AnalyticsDateHelper.resolveRange(start, end, 30)
        );
        assertEquals("Khoảng thời gian tối đa cho phép là 3 năm (1.095 ngày).", ex.getMessage());
    }

    @Test
    @DisplayName("Edge Case: startDate == endDate (Khoảng 1 ngày duy nhất) hợp lệ từ 00:00:00 đến 23:59:59")
    void testResolveRange_SingleDay() {
        LocalDate singleDay = LocalDate.now(AnalyticsDateHelper.VN_ZONE).minusDays(1);

        Instant[] range = AnalyticsDateHelper.resolveRange(singleDay, singleDay, 30);
        assertNotNull(range);
        assertEquals(2, range.length);
        assertTrue(range[0].isBefore(range[1]));
    }

    @Test
    @DisplayName("Edge Case: startDate == null và endDate == null -> Fallback về defaultDaysBack")
    void testResolveRange_DefaultRange() {
        Instant[] range = AnalyticsDateHelper.resolveRange(null, null, 30);
        assertNotNull(range);
        assertEquals(2, range.length);
        assertTrue(range[0].isBefore(range[1]));
    }

    @Test
    @DisplayName("Edge Case: Quý không hợp lệ (quarter < 1 hoặc quarter > 4) ném BadRequestException")
    void testResolveQuarterRange_InvalidQuarter() {
        assertThrows(BadRequestException.class, () -> AnalyticsDateHelper.resolveQuarterRange(2026, 0));
        assertThrows(BadRequestException.class, () -> AnalyticsDateHelper.resolveQuarterRange(2026, 5));
    }

    @Test
    @DisplayName("Happy Path: Quý 1, 2, 3, 4 trả về đúng mốc thời gian")
    void testResolveQuarterRange_AllQuarters() {
        for (int q = 1; q <= 4; q++) {
            Instant[] range = AnalyticsDateHelper.resolveQuarterRange(2026, q);
            assertNotNull(range);
            assertEquals(2, range.length);
            assertTrue(range[0].isBefore(range[1]));
        }
    }
}
