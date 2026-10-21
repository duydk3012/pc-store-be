package com.datn.pc_store.modules.auth.service;

import com.datn.pc_store.modules.auth.dto.AuthResponse;
import com.datn.pc_store.modules.auth.dto.LoginRequest;
import com.datn.pc_store.modules.auth.dto.RegisterRequest;
import com.datn.pc_store.modules.auth.entity.RefreshToken;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.auth.security.JwtService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

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

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        User user = userRepository.findByUsername(req.username()).orElseThrow();
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
