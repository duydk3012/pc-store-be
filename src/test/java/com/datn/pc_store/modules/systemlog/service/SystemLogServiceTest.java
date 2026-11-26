package com.datn.pc_store.modules.systemlog.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.dto.SystemLogFilterRequest;
import com.datn.pc_store.modules.systemlog.dto.SystemLogResponse;
import com.datn.pc_store.modules.systemlog.dto.SystemLogSummaryResponse;
import com.datn.pc_store.modules.systemlog.entity.SystemLog;
import com.datn.pc_store.modules.systemlog.enums.*;
import com.datn.pc_store.modules.systemlog.repository.SystemLogRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemLogServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @InjectMocks
    private SystemLogService systemLogService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("TC-01: Tự động trích xuất Actor (SecurityUtils) & HTTP Context (HttpRequestUtils)")
    void testSaveLog_AutoExtractActorAndHttpContext() {
        User adminUser = User.builder()
                .id(10001L)
                .username("demo_admin")
                .passwordHash("hashed")
                .role(RoleType.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        CustomUserDetails principal = new CustomUserDetails(adminUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/orders/22001/status");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(systemLogRepository.save(any(SystemLog.class))).thenAnswer(inv -> {
            SystemLog entity = inv.getArgument(0);
            entity.setId(10045L);
            return entity;
        });

        SystemLogCreateCommand cmd = SystemLogCreateCommand.builder()
                .module(SystemLogModule.ORDER)
                .action(SystemLogAction.UPDATE_STATUS)
                .resourceType("ORDER")
                .resourceId(22001L)
                .resourceCode("DEMO-PAY-COD-01")
                .description("Cập nhật trạng thái đơn hàng DEMO-PAY-COD-01 từ PENDING sang CONFIRMED")
                .detailMap(Map.of("oldStatus", "PENDING", "newStatus", "CONFIRMED"))
                .build();

        SystemLogResponse response = systemLogService.saveLog(cmd);

        assertNotNull(response);
        assertEquals(10045L, response.getId());
        assertEquals(10001L, response.getUserId());
        assertEquals("demo_admin", response.getUsername());
        assertEquals(RoleType.ADMIN, response.getUserRole());
        assertEquals(0, response.getUserRole().getValue());
        assertEquals(SystemLogModule.ORDER, response.getModule());
        assertEquals("Đơn bán hàng", response.getModuleLabel());
        assertEquals(SystemLogAction.UPDATE_STATUS, response.getAction());
        assertEquals("Chuyển trạng thái", response.getActionLabel());
        assertEquals(SystemLogStatus.SUCCESS, response.getStatus());
        assertEquals("PATCH", response.getHttpMethod());
        assertEquals("/api/orders/22001/status", response.getRequestUri());
        assertEquals("127.0.0.1", response.getIpAddress());
        assertThat(response.getDetailJson()).contains("\"oldStatus\":\"PENDING\"").contains("\"newStatus\":\"CONFIRMED\"");
    }

    @Test
    @DisplayName("TC-02: Ghi log từ Webhook không có JWT (userId = null)")
    void testSaveLog_WebhookWithoutJwt() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments/zalopay/callback");
        request.addHeader("X-Forwarded-For", "113.160.92.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(systemLogRepository.save(any(SystemLog.class))).thenAnswer(inv -> {
            SystemLog entity = inv.getArgument(0);
            entity.setId(10046L);
            return entity;
        });

        SystemLogCreateCommand cmd = SystemLogCreateCommand.builder()
                .module(SystemLogModule.PAYMENT)
                .action(SystemLogAction.PAYMENT_WEBHOOK)
                .status(SystemLogStatus.FAILED)
                .resourceType("PAYMENT")
                .description("Cảnh báo: ZaloPay Webhook Callback có chữ ký MAC không hợp lệ")
                .detailMap(Map.of("reason", "mac not equal", "mac", "secret-mac", "key2", "secret-key2"))
                .build();

        SystemLogResponse response = systemLogService.saveLog(cmd);

        assertNotNull(response);
        assertNull(response.getUserId());
        assertNull(response.getUsername());
        assertNull(response.getUserRole());
        assertEquals(SystemLogModule.PAYMENT, response.getModule());
        assertEquals(9, response.getModule().getValue());
        assertEquals(SystemLogAction.PAYMENT_WEBHOOK, response.getAction());
        assertEquals(8, response.getAction().getValue());
        assertEquals(SystemLogStatus.FAILED, response.getStatus());
        assertEquals(0, response.getStatus().getValue());
        assertEquals("113.160.92.10", response.getIpAddress());
        assertEquals("POST", response.getHttpMethod());
        assertEquals("/api/payments/zalopay/callback", response.getRequestUri());
        assertThat(response.getDetailJson()).contains("mac not equal")
                .doesNotContain("secret-mac")
                .doesNotContain("secret-key2");
    }

    @Test
    @DisplayName("TC-03: Ghi log từ Cronjob không có HTTP Request (ipAddress = null, requestUri = null)")
    void testSaveLog_ScheduledJobWithoutHttpRequest() {
        when(systemLogRepository.save(any(SystemLog.class))).thenAnswer(inv -> {
            SystemLog entity = inv.getArgument(0);
            entity.setId(10047L);
            return entity;
        });

        SystemLogCreateCommand cmd = SystemLogCreateCommand.builder()
                .username("SYSTEM")
                .module(SystemLogModule.SYSTEM)
                .action(SystemLogAction.SCHEDULED_JOB)
                .status(SystemLogStatus.SUCCESS)
                .resourceType("REFRESH_TOKEN")
                .resourceCode("RefreshTokenCleanupJob")
                .description("Chạy tác vụ định kỳ dọn dẹp refresh token hết hạn")
                .detailMap(Map.of("deletedCount", 12))
                .build();

        SystemLogResponse response = systemLogService.saveLog(cmd);

        assertNotNull(response);
        assertNull(response.getUserId());
        assertEquals("SYSTEM", response.getUsername());
        assertNull(response.getIpAddress());
        assertNull(response.getRequestUri());
        assertNull(response.getHttpMethod());
        assertEquals(SystemLogModule.SYSTEM, response.getModule());
        assertEquals(12, response.getModule().getValue());
        assertEquals(SystemLogAction.SCHEDULED_JOB, response.getAction());
        assertEquals(10, response.getAction().getValue());
    }

    @Test
    @DisplayName("TC-06: Kiểm tra trường sắp xếp không hợp lệ ném BadRequestException")
    void testGetLogs_InvalidSortField_ThrowsBadRequest() {
        Pageable invalidPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "invalidField"));
        assertThrows(BadRequestException.class,
                () -> systemLogService.getLogs(new SystemLogFilterRequest(), invalidPageable));
    }

    @Test
    @DisplayName("TC-07: Không rò rỉ dữ liệu nhạy cảm (password, newPassword, refreshToken, code)")
    void testSaveLog_MasksSensitiveFieldsInDetailJson() {
        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        when(systemLogRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        SystemLogCreateCommand cmd = SystemLogCreateCommand.builder()
                .module(SystemLogModule.AUTH)
                .action(SystemLogAction.LOGIN)
                .description("Đăng nhập & đổi mật khẩu")
                .detailMap(Map.of(
                        "username", "demo_customer01",
                        "password", "SecretPass@123",
                        "newPassword", "NewSecret@456",
                        "refreshToken", "raw-refresh-token-value",
                        "code", "123456"
                ))
                .build();

        systemLogService.saveLog(cmd);

        SystemLog saved = captor.getValue();
        assertNotNull(saved.getDetailJson());
        assertThat(saved.getDetailJson())
                .contains("demo_customer01")
                .doesNotContain("password")
                .doesNotContain("newPassword")
                .doesNotContain("refreshToken")
                .doesNotContain("code")
                .doesNotContain("SecretPass@123")
                .doesNotContain("NewSecret@456")
                .doesNotContain("raw-refresh-token-value")
                .doesNotContain("123456");
    }

    @Test
    @DisplayName("An toàn Transaction: Lỗi DB khi lưu log không ném exception ra ngoài")
    void testSaveLog_DatabaseFailureIsFaultTolerant() {
        when(systemLogRepository.save(any(SystemLog.class))).thenThrow(new RuntimeException("DB connection lost"));

        SystemLogCreateCommand cmd = SystemLogCreateCommand.builder()
                .module(SystemLogModule.ORDER)
                .action(SystemLogAction.CREATE)
                .description("Tạo đơn hàng")
                .build();

        SystemLogResponse res = assertDoesNotThrow(() -> systemLogService.saveLog(cmd));
        assertNull(res);
    }

    @Test
    @DisplayName("Converter & Summary: Kiểm chứng JPA Converter TINYINT UNSIGNED và thống kê tổng quan")
    void testConvertersAndGetSummary() {
        SystemLogModuleConverter modConv = new SystemLogModuleConverter();
        SystemLogActionConverter actConv = new SystemLogActionConverter();
        SystemLogStatusConverter stConv = new SystemLogStatusConverter();

        assertEquals((byte) 8, modConv.convertToDatabaseColumn(SystemLogModule.ORDER));
        assertEquals(SystemLogModule.ORDER, modConv.convertToEntityAttribute((byte) 8));
        assertEquals((byte) 6, actConv.convertToDatabaseColumn(SystemLogAction.ASSIGN_SERIAL));
        assertEquals(SystemLogAction.ASSIGN_SERIAL, actConv.convertToEntityAttribute((byte) 6));
        assertEquals((byte) 1, stConv.convertToDatabaseColumn(SystemLogStatus.SUCCESS));
        assertEquals(SystemLogStatus.FAILED, stConv.convertToEntityAttribute((byte) 0));

        List<Object[]> statusRows = new ArrayList<>();
        statusRows.add(new Object[]{SystemLogStatus.SUCCESS, 8L});
        statusRows.add(new Object[]{SystemLogStatus.FAILED, 2L});

        List<Object[]> moduleRows = new ArrayList<>();
        moduleRows.add(new Object[]{SystemLogModule.ORDER, 5L});
        moduleRows.add(new Object[]{SystemLogModule.PAYMENT, 3L});

        List<Object[]> actionRows = new ArrayList<>();
        actionRows.add(new Object[]{SystemLogAction.UPDATE_STATUS, 4L});

        when(systemLogRepository.countGroupedByStatus(null, null)).thenReturn(statusRows);
        when(systemLogRepository.countGroupedByModule(null, null)).thenReturn(moduleRows);
        when(systemLogRepository.countGroupedByAction(null, null)).thenReturn(actionRows);

        SystemLogSummaryResponse summary = systemLogService.getSummary(null, null);

        assertEquals(10L, summary.getTotalLogs());
        assertEquals(8L, summary.getSuccessCount());
        assertEquals(2L, summary.getFailedCount());
        assertEquals(80.0, summary.getSuccessRate());
        assertEquals(SystemLogModule.values().length, summary.getModuleBreakdown().size());
        assertEquals(SystemLogAction.values().length, summary.getActionBreakdown().size());
    }

    @Test
    @DisplayName("TC-04 & TC-05: getLogs và getLogsByResource trả về PageResponse chuẩn")
    @SuppressWarnings("unchecked")
    void testGetLogsAndByResource() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdDate"));
        SystemLog logEntity = SystemLog.builder()
                .id(10045L)
                .userId(10001L)
                .username("demo_admin")
                .userRole(RoleType.ADMIN)
                .module(SystemLogModule.ORDER)
                .action(SystemLogAction.UPDATE_STATUS)
                .status(SystemLogStatus.SUCCESS)
                .resourceType("ORDER")
                .resourceId(22001L)
                .resourceCode("DEMO-PAY-COD-01")
                .description("Cập nhật trạng thái đơn hàng DEMO-PAY-COD-01")
                .createdDate(Instant.parse("2026-11-23T15:10:00Z"))
                .build();

        when(systemLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(logEntity), pageable, 1));
        when(systemLogRepository.findByResourceTypeIgnoreCaseAndResourceId("ORDER", 22001L, pageable))
                .thenReturn(new PageImpl<>(List.of(logEntity), pageable, 1));

        SystemLogFilterRequest filter = SystemLogFilterRequest.builder()
                .module(SystemLogModule.ORDER)
                .status(SystemLogStatus.SUCCESS)
                .build();

        PageResponse<SystemLogResponse> pageRes = systemLogService.getLogs(filter, pageable);
        assertEquals(1, pageRes.getTotalElements());
        assertEquals(SystemLogModule.ORDER, pageRes.getContent().get(0).getModule());

        PageResponse<SystemLogResponse> auditTrail = systemLogService.getLogsByResource("ORDER", 22001L, pageable);
        assertEquals(1, auditTrail.getTotalElements());
        assertEquals(22001L, auditTrail.getContent().get(0).getResourceId());
    }

    @Test
    @DisplayName("SystemLogAspect: getOrder() và getAdvice() không gây đệ quy StackOverflowError khi kết hợp nhiều Advisor")
    void testSystemLogAspect_GetOrderDoesNotRecurse() {
        com.datn.pc_store.modules.systemlog.annotation.SystemLogAspect aspect =
                new com.datn.pc_store.modules.systemlog.annotation.SystemLogAspect(null);

        assertEquals(org.springframework.core.Ordered.LOWEST_PRECEDENCE, aspect.getOrder());
        assertNotNull(aspect.getAdvice());
        assertNotSame(aspect, aspect.getAdvice());
        assertNotNull(aspect.getPointcut());
    }
}
