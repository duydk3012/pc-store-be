package com.datn.pc_store.modules.systemlog.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.systemlog.dto.SystemLogFilterRequest;
import com.datn.pc_store.modules.systemlog.dto.SystemLogResponse;
import com.datn.pc_store.modules.systemlog.dto.SystemLogSummaryResponse;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.systemlog.service.SystemLogService;
import com.datn.pc_store.modules.user.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SystemLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SystemLogService systemLogService;

    @InjectMocks
    private SystemLogController systemLogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(systemLogController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("TC-04: GET /api/admin/system-logs?module=ORDER&status=SUCCESS trả về danh sách phân trang")
    void testGetLogsFilteredByModuleAndStatus() throws Exception {
        SystemLogResponse logItem = SystemLogResponse.builder()
                .id(10045L)
                .userId(10001L)
                .username("demo_admin")
                .userRole(RoleType.ADMIN)
                .module(SystemLogModule.ORDER)
                .moduleLabel("Đơn bán hàng")
                .action(SystemLogAction.UPDATE_STATUS)
                .actionLabel("Chuyển trạng thái")
                .status(SystemLogStatus.SUCCESS)
                .resourceType("ORDER")
                .resourceId(22001L)
                .resourceCode("DEMO-PAY-COD-01")
                .description("Cập nhật trạng thái đơn hàng DEMO-PAY-COD-01 từ PENDING sang CONFIRMED")
                .detailJson("{\"oldStatus\":\"PENDING\",\"newStatus\":\"CONFIRMED\"}")
                .ipAddress("127.0.0.1")
                .httpMethod("PATCH")
                .requestUri("/api/orders/22001/status")
                .createdDate(Instant.parse("2026-11-23T15:10:00Z"))
                .build();

        PageResponse<SystemLogResponse> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(logItem), PageRequest.of(0, 20), 1)
        );

        when(systemLogService.getLogs(any(SystemLogFilterRequest.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/system-logs")
                        .param("module", "ORDER")
                        .param("status", "SUCCESS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10045))
                .andExpect(jsonPath("$.content[0].username").value("demo_admin"))
                .andExpect(jsonPath("$.content[0].module").value("ORDER"))
                .andExpect(jsonPath("$.content[0].moduleLabel").value("Đơn bán hàng"))
                .andExpect(jsonPath("$.content[0].action").value("UPDATE_STATUS"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].resourceCode").value("DEMO-PAY-COD-01"));
    }

    @Test
    @DisplayName("TC-05: GET /api/admin/system-logs/by-resource/ORDER/22001 trả về Audit Trail của chứng từ")
    void testGetLogsByResource() throws Exception {
        SystemLogResponse logItem = SystemLogResponse.builder()
                .id(10045L)
                .module(SystemLogModule.ORDER)
                .action(SystemLogAction.UPDATE_STATUS)
                .status(SystemLogStatus.SUCCESS)
                .resourceType("ORDER")
                .resourceId(22001L)
                .resourceCode("DEMO-PAY-COD-01")
                .description("Cập nhật trạng thái đơn hàng DEMO-PAY-COD-01")
                .build();

        PageResponse<SystemLogResponse> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(logItem), PageRequest.of(0, 20), 1)
        );

        when(systemLogService.getLogsByResource(eq("ORDER"), eq(22001L), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/system-logs/by-resource/ORDER/22001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].resourceType").value("ORDER"))
                .andExpect(jsonPath("$.content[0].resourceId").value(22001));
    }

    @Test
    @DisplayName("TC-06: GET /api/admin/system-logs?sort=invalidField,desc trả về HTTP 400 Bad Request")
    void testGetLogsInvalidSortFieldReturnsBadRequest() throws Exception {
        when(systemLogService.getLogs(any(SystemLogFilterRequest.class), any(Pageable.class)))
                .thenThrow(new BadRequestException("Trường sắp xếp không hợp lệ: invalidField"));

        mockMvc.perform(get("/api/admin/system-logs")
                        .param("sort", "invalidField,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/admin/system-logs/{id} và /summary hoạt động chính xác")
    void testGetLogByIdAndSummary() throws Exception {
        SystemLogResponse detail = SystemLogResponse.builder()
                .id(10001L)
                .username("demo_admin")
                .module(SystemLogModule.AUTH)
                .action(SystemLogAction.LOGIN)
                .status(SystemLogStatus.SUCCESS)
                .description("Người dùng demo_admin đăng nhập hệ thống thành công")
                .build();

        when(systemLogService.getLogById(10001L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/system-logs/10001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10001))
                .andExpect(jsonPath("$.module").value("AUTH"));

        SystemLogSummaryResponse summary = SystemLogSummaryResponse.builder()
                .totalLogs(10L)
                .successCount(9L)
                .failedCount(1L)
                .successRate(90.0)
                .moduleBreakdown(List.of(
                        SystemLogSummaryResponse.ModuleStatItem.builder()
                                .module(SystemLogModule.ORDER)
                                .moduleLabel("Đơn bán hàng")
                                .count(4L)
                                .build()
                ))
                .actionBreakdown(List.of())
                .build();

        when(systemLogService.getSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/admin/system-logs/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").value(10))
                .andExpect(jsonPath("$.successCount").value(9))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.successRate").value(90.0));
    }
}
