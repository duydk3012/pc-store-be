package com.datn.pc_store.common.util;

import com.datn.pc_store.common.exception.BadRequestException;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;

/**
 * Tiện ích kiểm tra tham số sắp xếp và khoảng thời gian lọc dùng chung cho phân trang.
 */
public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static void validateSort(Pageable pageable, Collection<String> allowedFields) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return;
        }
        for (var sort : pageable.getSort()) {
            if (!allowedFields.contains(sort.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + sort.getProperty()
                        + ". Trường cho phép: " + String.join(", ", allowedFields));
            }
        }
    }

    public static void validateDateRange(Instant fromDate, Instant toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("Thời điểm bắt đầu không được lớn hơn thời điểm kết thúc.");
        }
    }
}
