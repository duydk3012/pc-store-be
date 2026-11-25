package com.datn.pc_store.common.util;

import com.datn.pc_store.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationUtilsTest {

    private static final Set<String> ALLOWED = Set.of("id", "createdDate", "status");

    @Test
    @DisplayName("validateSort chấp nhận các trường hợp lệ")
    void validateSort_validFields() {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate", "id"));
        assertDoesNotThrow(() -> PaginationUtils.validateSort(pageable, ALLOWED));
    }

    @Test
    @DisplayName("validateSort ném BadRequestException khi trường sắp xếp không hợp lệ")
    void validateSort_invalidField() {
        var pageable = PageRequest.of(0, 10, Sort.by("unknownField"));
        assertThrows(BadRequestException.class, () -> PaginationUtils.validateSort(pageable, ALLOWED));
    }

    @Test
    @DisplayName("validateDateRange ném BadRequestException khi fromDate sau toDate")
    void validateDateRange_invalidRange() {
        Instant from = Instant.parse("2026-11-23T12:00:00Z");
        Instant to = Instant.parse("2026-11-23T10:00:00Z");
        assertThrows(BadRequestException.class, () -> PaginationUtils.validateDateRange(from, to));
    }

    @Test
    @DisplayName("validateDateRange chấp nhận khoảng thời gian hợp lệ hoặc null")
    void validateDateRange_valid() {
        Instant from = Instant.parse("2026-11-23T10:00:00Z");
        Instant to = Instant.parse("2026-11-23T12:00:00Z");
        assertDoesNotThrow(() -> PaginationUtils.validateDateRange(from, to));
        assertDoesNotThrow(() -> PaginationUtils.validateDateRange(null, to));
        assertDoesNotThrow(() -> PaginationUtils.validateDateRange(from, null));
    }
}
