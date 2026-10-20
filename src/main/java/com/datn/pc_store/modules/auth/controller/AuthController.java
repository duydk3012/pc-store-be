package com.datn.pc_store.modules.auth.controller;

import com.datn.pc_store.modules.auth.dto.AuthResponse;
import com.datn.pc_store.modules.auth.dto.LoginRequest;
import com.datn.pc_store.modules.auth.dto.RefreshTokenRequest;
import com.datn.pc_store.modules.auth.dto.RegisterRequest;
import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req
            ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest req
            ) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal CustomUserDetails principal) {
        authService.logoutAll(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Map<String, Object> me(
            @AuthenticationPrincipal CustomUserDetails principal
            ) {
        return Map.of(
                "id", principal.getId(),
                "username", principal.getUsername(),
                "authorities", principal.getAuthorities().stream()
                        .map(Object::toString).toList()
        );
    }
}
