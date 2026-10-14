package com.datn.pc_store.modules.auth.service;

import com.datn.pc_store.modules.auth.dto.AuthResponse;
import com.datn.pc_store.modules.auth.dto.LoginRequest;
import com.datn.pc_store.modules.auth.dto.RegisterRequest;
import com.datn.pc_store.modules.auth.entity.User;
import com.datn.pc_store.modules.auth.enums.RoleType;
import com.datn.pc_store.modules.auth.enums.UserStatus;
import com.datn.pc_store.modules.auth.repository.UserRepository;
import com.datn.pc_store.modules.auth.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại!");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại!");
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
                .build()
        );
        return toResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        User user = userRepository.findByUsername(req.username()).orElseThrow();
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(
                jwtService.generateToken(u),
                "Bearer",
                jwtService.getExpirationMs(),
                u.getUsername(),
                u.getFullName(),
                u.getRole()
        );
    }

}
