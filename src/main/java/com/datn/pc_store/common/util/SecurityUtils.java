package com.datn.pc_store.common.util;

import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.user.enums.RoleType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

/**
 * Tiện ích truy xuất thông tin người dùng đang đăng nhập từ SecurityContext.
 */
public final class SecurityUtils {

    private static final Set<String> STAFF_OR_ADMIN_AUTHORITIES = Set.of(
            "ROLE_ADMIN",
            "ROLE_MANAGER",
            "ROLE_STAFF_SALES",
            "ROLE_STAFF_WAREHOUSE",
            "ROLE_STAFF_TECH"
    );

    private SecurityUtils() {
    }

    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static Optional<CustomUserDetails> getCurrentUser() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(CustomUserDetails.class::isInstance)
                .map(CustomUserDetails.class::cast);
    }

    public static CustomUserDetails requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Yêu cầu đăng nhập"));
    }

    public static Long getCurrentUserId() {
        return getCurrentUser()
                .map(CustomUserDetails::getId)
                .orElse(null);
    }

    public static String getCurrentUsername() {
        return getCurrentUser()
                .map(CustomUserDetails::getUsername)
                .orElse(null);
    }

    public static RoleType getCurrentUserRole() {
        return getCurrentUser()
                .map(CustomUserDetails::getRole)
                .orElse(null);
    }

    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(target)))
                .orElse(false);
    }

    public static boolean isStaffOrAdmin() {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> STAFF_OR_ADMIN_AUTHORITIES.contains(a.getAuthority())))
                .orElse(false);
    }
}
