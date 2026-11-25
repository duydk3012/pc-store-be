package com.datn.pc_store.modules.systemlog.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.systemlog.dto.SystemLogFilterRequest;
import com.datn.pc_store.modules.systemlog.dto.SystemLogResponse;
import com.datn.pc_store.modules.systemlog.dto.SystemLogSummaryResponse;
import com.datn.pc_store.modules.systemlog.service.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/system-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "System Logs (Admin)", description = "Tra cứu nhật ký hệ thống & Kiểm toán hoạt động (Audit Trail)")
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @Operation(summary = "Tra cứu, lọc đa chiều và phân trang nhật ký hệ thống")
    public ResponseEntity<PageResponse<SystemLogResponse>> getLogs(
            @ParameterObject SystemLogFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(systemLogService.getLogs(filter, pageable));
    }

    @GetMapping("/summary")
    @Operation(summary = "Thống kê nhanh tổng số log, tỷ lệ thành công/thất bại và phân bổ theo phân hệ")
    public ResponseEntity<SystemLogSummaryResponse> getSummary(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate
    ) {
        return ResponseEntity.ok(systemLogService.getSummary(fromDate, toDate));
    }

    @GetMapping("/by-resource/{resourceType}/{resourceId}")
    @Operation(summary = "Xem lịch sử kiểm toán (Audit Trail) của một chứng từ/bản ghi cụ thể")
    public ResponseEntity<PageResponse<SystemLogResponse>> getLogsByResource(
            @PathVariable String resourceType,
            @PathVariable Long resourceId,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(systemLogService.getLogsByResource(resourceType, resourceId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản ghi nhật ký hệ thống")
    public ResponseEntity<SystemLogResponse> getLogById(@PathVariable Long id) {
        return ResponseEntity.ok(systemLogService.getLogById(id));
    }
}
