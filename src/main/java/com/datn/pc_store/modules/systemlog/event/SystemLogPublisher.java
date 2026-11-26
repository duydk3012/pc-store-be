package com.datn.pc_store.modules.systemlog.event;

import com.datn.pc_store.common.util.HttpRequestUtils;
import com.datn.pc_store.common.util.SecurityUtils;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Component phát sự kiện ghi nhật ký hệ thống, tự động đính kèm ngữ cảnh người thao tác
 * (SecurityUtils), ngữ cảnh HTTP (HttpRequestUtils) và lọc bỏ các trường nhạy cảm.
 */
@Slf4j
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@RequiredArgsConstructor
public class SystemLogPublisher {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "passwordhash",
            "token",
            "accesstoken",
            "refreshtoken",
            "tokenhash",
            "code",
            "otp",
            "verifycode",
            "key1",
            "key2",
            "mac",
            "secret",
            "authorization"
    );

    private final ApplicationEventPublisher eventPublisher;

    public void publish(SystemLogCreateCommand command) {
        if (command == null || command.getModule() == null || command.getAction() == null) {
            return;
        }
        try {
            enrichContextAndMaskSensitiveData(command);
            eventPublisher.publishEvent(new SystemLogEvent(this, command));
        } catch (Exception ex) {
            log.error("Không thể phát sự kiện ghi nhật ký hệ thống: {}", ex.getMessage(), ex);
        }
    }

    public static void enrichContextAndMaskSensitiveData(SystemLogCreateCommand command) {
        if (command.getUserId() == null) {
            command.setUserId(SecurityUtils.getCurrentUserId());
        }
        if (command.getUsername() == null || command.getUsername().isBlank()) {
            command.setUsername(SecurityUtils.getCurrentUsername());
        }
        if (command.getUserRole() == null) {
            command.setUserRole(SecurityUtils.getCurrentUserRole());
        }
        if (command.getStatus() == null) {
            command.setStatus(SystemLogStatus.SUCCESS);
        }
        if (command.getIpAddress() == null) {
            command.setIpAddress(HttpRequestUtils.getClientIpAddress());
        }
        if (command.getUserAgent() == null) {
            command.setUserAgent(HttpRequestUtils.getUserAgent());
        }
        if (command.getHttpMethod() == null) {
            command.setHttpMethod(HttpRequestUtils.getHttpMethod());
        }
        if (command.getRequestUri() == null) {
            command.setRequestUri(HttpRequestUtils.getRequestUri());
        }

        if (command.getDetailMap() != null && !command.getDetailMap().isEmpty()) {
            Map<String, Object> sanitized = sanitizeMap(command.getDetailMap());
            command.setDetailMap(sanitized);
            if (command.getDetailJson() == null || command.getDetailJson().isBlank()) {
                command.setDetailJson(serializeToJson(sanitized));
            }
        } else if (command.getDetailJson() != null && !command.getDetailJson().isBlank()) {
            command.setDetailJson(sanitizeJsonString(command.getDetailJson()));
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> sanitizeMap(Map<String, ?> source) {
        Map<String, Object> clean = new LinkedHashMap<>();
        if (source == null) {
            return clean;
        }
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || isSensitiveKey(key)) {
                continue;
            }
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> nestedMap) {
                clean.put(key, sanitizeMap((Map<String, Object>) nestedMap));
            } else {
                clean.put(key, val);
            }
        }
        return clean;
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.replace("_", "").replace("-", "").trim().toLowerCase();
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.contains("password")
                || normalized.contains("refreshtoken")
                || normalized.contains("accesstoken")
                || normalized.contains("secretkey");
    }

    private static String serializeToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Không thể tuần tự hóa detailMap sang JSON: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String sanitizeJsonString(String rawJson) {
        try {
            Object parsed = OBJECT_MAPPER.readValue(rawJson, Object.class);
            if (parsed instanceof Map<?, ?> map) {
                Map<String, Object> clean = sanitizeMap((Map<String, Object>) map);
                return clean.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(clean);
            }
            return rawJson;
        } catch (Exception e) {
            return rawJson;
        }
    }
}
