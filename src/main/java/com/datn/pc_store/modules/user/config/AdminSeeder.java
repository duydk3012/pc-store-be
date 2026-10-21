package com.datn.pc_store.modules.user.config;

import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD trống, bỏ qua tạo tài khoản admin.");
            return;
        }
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        userRepository.save(User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName("Quản trị viên")
                .email("admin@pcstore.local")
                .role(RoleType.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("Đã tạo tài khoản admin mặc định.");
    }

}
