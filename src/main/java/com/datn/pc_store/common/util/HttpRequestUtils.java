package com.datn.pc_store.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Tiện ích trích xuất thông tin ngữ cảnh HTTP Request hiện tại (IP, User-Agent, Method, URI)
 * phục vụ ghi nhận nhật ký hệ thống (System Log). An toàn khi gọi từ luồng background/scheduler.
 */
public final class HttpRequestUtils {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_URI_LENGTH = 255;

    private HttpRequestUtils() {
    }

    public static Optional<HttpServletRequest> getCurrentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    public static String getClientIpAddress() {
        return getCurrentRequest()
                .map(HttpRequestUtils::extractClientIp)
                .orElse(null);
    }

    public static String getUserAgent() {
        return getCurrentRequest()
                .map(req -> truncate(req.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH))
                .orElse(null);
    }

    public static String getHttpMethod() {
        return getCurrentRequest()
                .map(HttpServletRequest::getMethod)
                .orElse(null);
    }

    public static String getRequestUri() {
        return getCurrentRequest()
                .map(req -> truncate(req.getRequestURI(), MAX_URI_LENGTH))
                .orElse(null);
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff.trim())) {
            String firstIp = xff.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return truncate(firstIp, MAX_IP_LENGTH);
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp.trim())) {
            return truncate(realIp.trim(), MAX_IP_LENGTH);
        }
        return truncate(request.getRemoteAddr(), MAX_IP_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
