package com.datn.pc_store.modules.auth.service;

import com.datn.pc_store.modules.auth.dto.AuthResponse;
import com.datn.pc_store.modules.auth.dto.LoginRequest;
import com.datn.pc_store.modules.auth.dto.RegisterRequest;
import com.datn.pc_store.modules.auth.entity.RefreshToken;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.auth.security.JwtService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SystemLogPublisher systemLogPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username đã tồn tại!"
            );
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email đã tồn tại!"
            );
        }

        User user = userRepository.save(User.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .email(req.email())
                .phoneNumber(req.phoneNumber())
                .address(req.address())
                .role(RoleType.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .userRole(user.getRole())
                    .module(SystemLogModule.AUTH)
                    .action(SystemLogAction.CREATE)
                    .status(SystemLogStatus.SUCCESS)
                    .resourceType("USER")
                    .resourceId(user.getId())
                    .resourceCode(user.getUsername())
                    .description("Đăng ký tài khoản khách hàng mới: " + user.getUsername())
                    .detailMap(Map.of("username", user.getUsername(), "email", user.getEmail(), "role", user.getRole().name()))
                    .build());
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );
        } catch (AuthenticationException ex) {
            if (systemLogPublisher != null) {
                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .username(req.username())
                        .module(SystemLogModule.AUTH)
                        .action(SystemLogAction.LOGIN)
                        .status(SystemLogStatus.FAILED)
                        .resourceType("USER")
                        .resourceCode(req.username())
                        .description("Đăng nhập thất bại với tài khoản " + req.username())
                        .detailMap(Map.of("username", req.username(), "reason", ex.getClass().getSimpleName()))
                        .build());
            }
            throw ex;
        }
        User user = userRepository.findByUsername(req.username()).orElseThrow();
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .userRole(user.getRole())
                    .module(SystemLogModule.AUTH)
                    .action(SystemLogAction.LOGIN)
                    .status(SystemLogStatus.SUCCESS)
                    .resourceType("USER")
                    .resourceId(user.getId())
                    .resourceCode(user.getUsername())
                    .description("Người dùng " + user.getUsername() + " đăng nhập hệ thống thành công")
                    .detailMap(Map.of("username", user.getUsername(), "role", user.getRole().name()))
                    .build());
        }
        return issueTokens(user);
    }

    /** noRollbackFor: để việc thu hồi hàng loạt khi phát hiện dùng lại token vẫn được lưu. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshToken stored = refreshTokenService.findByRaw(rawToken)
                .orElseThrow(() -> unauthorized("Refresh token không hợp lệ"));
        if (stored.isRevoked()) {
            // Token đã dùng/thu hồi mà bị dùng lại -> nghi bị đánh cắp
            refreshTokenService.revokeAllByUser(stored.getUserId());
            throw unauthorized("Refresh token đã bị thu hồi!");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw unauthorized("Refresh token đã hết hạn!");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> unauthorized("Người dùng không tồn tại!"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không còn hoạt động!");
        }

        stored.setRevoked(true); // xoay vòng: token cũ hết giá trị
        return issueTokens(user);
    }

    /** Đăng xuất thiết bị hiện tại. Luôn thành công (idempotent). */
    @Transactional
    public void logout(String rawToken) {
        refreshTokenService.findByRaw(rawToken).ifPresent(t -> t.setRevoked(true));
    }

    /** Đăng xuất mọi thiết bị. */
    @Transactional
    public void logoutAll(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAllByUser(userId);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .userRole(user.getRole())
                    .module(SystemLogModule.AUTH)
                    .action(SystemLogAction.LOGOUT)
                    .status(SystemLogStatus.SUCCESS)
                    .resourceType("USER")
                    .resourceId(user.getId())
                    .resourceCode(user.getUsername())
                    .description("Thu hồi toàn bộ phiên đăng nhập của tài khoản " + user.getUsername())
                    .build());
        }
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                refreshTokenService.issue(user.getId()),
                "Bearer",
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    private static ResponseStatusException unauthorized(String msg) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
    }

}
