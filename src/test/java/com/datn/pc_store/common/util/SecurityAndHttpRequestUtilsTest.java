package com.datn.pc_store.common.util;

import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndHttpRequestUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("SecurityUtils trích xuất đầy đủ userId, username, role và quyền staff/admin")
    void securityUtils_extractsCurrentUserMetadata() {
        User user = User.builder()
                .id(10001L)
                .username("demo_admin")
                .fullName("Quản trị viên Demo")
                .role(RoleType.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(10001L, SecurityUtils.getCurrentUserId());
        assertEquals("demo_admin", SecurityUtils.getCurrentUsername());
        assertEquals(RoleType.ADMIN, SecurityUtils.getCurrentUserRole());
        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertTrue(SecurityUtils.isStaffOrAdmin());
    }

    @Test
    @DisplayName("HttpRequestUtils trích xuất IP qua X-Forwarded-For, User-Agent, Method và URI")
    void httpRequestUtils_extractsRequestMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/orders/22001/status");
        request.addHeader("X-Forwarded-For", "203.113.148.10, 10.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 PCStore-Test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("203.113.148.10", HttpRequestUtils.getClientIpAddress());
        assertEquals("Mozilla/5.0 PCStore-Test", HttpRequestUtils.getUserAgent());
        assertEquals("PATCH", HttpRequestUtils.getHttpMethod());
        assertEquals("/api/orders/22001/status", HttpRequestUtils.getRequestUri());
    }

    @Test
    @DisplayName("HttpRequestUtils trả về null an toàn khi gọi từ luồng không có HTTP Request (scheduler)")
    void httpRequestUtils_returnsNullWhenNoRequestContext() {
        assertNull(HttpRequestUtils.getClientIpAddress());
        assertNull(HttpRequestUtils.getUserAgent());
        assertNull(HttpRequestUtils.getHttpMethod());
        assertNull(HttpRequestUtils.getRequestUri());
    }
}
