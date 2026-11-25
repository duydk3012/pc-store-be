package com.datn.pc_store.modules.user.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.common.util.PaginationUtils;
import com.datn.pc_store.modules.auth.service.RefreshTokenService;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.datn.pc_store.modules.user.dto.*;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.mapper.UserMapper;
import com.datn.pc_store.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "username", "fullName", "email", "phoneNumber", "address",
            "citizenIdentificationCard", "role", "status", "createdDate", "createdBy",
            "lastModifiedDate", "lastModifiedBy"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final SystemLogPublisher systemLogPublisher;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(String keyword, RoleType role, UserStatus status, Pageable pageable) {
        PaginationUtils.validateSort(pageable, SORTABLE_FIELDS);
        return PageResponse.of(userRepository.searchUsers(keyword, role, status, pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<UserRoleCountResponse> countUsersByRole(RoleType roleFilter, UserStatus status) {
        List<Object[]> results = userRepository.countUsersByRoleGrouped(roleFilter, status);
        Map<RoleType, Long> countMap = new EnumMap<>(RoleType.class);

        if (results != null) {
            for (Object[] row : results) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    RoleType role = null;
                    if (row[0] instanceof RoleType rt) {
                        role = rt;
                    } else if (row[0] instanceof Number num) {
                        role = RoleType.fromValue(num.intValue());
                    }
                    if (role != null) {
                        long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                        countMap.put(role, count);
                    }
                }
            }
        }

        RoleType[] targetRoles = (roleFilter != null)
                ? new RoleType[]{roleFilter}
                : RoleType.values();

        List<UserRoleCountResponse> response = new ArrayList<>();
        for (RoleType role : targetRoles) {
            response.add(UserRoleCountResponse.builder()
                    .role(role)
                    .label(role.getLabel())
                    .count(countMap.getOrDefault(role, 0L))
                    .build());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public long countUsers(RoleType role, UserStatus status) {
        return userRepository.countUsers(role, status);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("Người dùng", "username", req.getUsername());
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Người dùng", "email", req.getEmail());
        }

        User user = userMapper.toEntity(req);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole() != null ? req.getRole() : RoleType.CUSTOMER);
        user.setStatus(req.getStatus() != null ? req.getStatus() : UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        log.info("Đã tạo người dùng id: {}", saved.getId());
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.USER)
                    .action(SystemLogAction.CREATE)
                    .resourceType("USER")
                    .resourceId(saved.getId())
                    .resourceCode(saved.getUsername())
                    .description("Tạo tài khoản người dùng mới: " + saved.getUsername())
                    .detailMap(Map.of("username", saved.getUsername(), "role", saved.getRole().name(), "status", saved.getStatus().name()))
                    .build());
        }
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest req) {
        User user = findUser(id);
        if (userRepository.existsByUsernameAndIdNot(req.getUsername(), id)) {
            throw new DuplicateResourceException("Người dùng", "username", req.getUsername());
        }
        if (userRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
            throw new DuplicateResourceException("Người dùng", "email", req.getEmail());
        }

        RoleType oldRole = user.getRole();
        UserStatus oldStatus = user.getStatus();

        boolean revokeSessions = !user.getUsername().equals(req.getUsername())
                || req.getPassword() != null
                || (req.getRole() != null && req.getRole() != user.getRole())
                || (req.getStatus() != null && req.getStatus() != user.getStatus());

        if (!user.getEmail().equalsIgnoreCase(req.getEmail())) {
            user.setEmailVerified(false);
        }
        userMapper.updateEntityFromDto(req, user);
        if (req.getPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (revokeSessions) {
            revokeSessions(user);
        }

        User updated = userRepository.save(user);
        log.info("Đã cập nhật người dùng id: {}", id);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.USER)
                    .action(SystemLogAction.UPDATE)
                    .resourceType("USER")
                    .resourceId(updated.getId())
                    .resourceCode(updated.getUsername())
                    .description("Cập nhật thông tin tài khoản người dùng " + updated.getUsername())
                    .detailMap(Map.of(
                            "oldRole", oldRole != null ? oldRole.name() : "",
                            "newRole", updated.getRole() != null ? updated.getRole().name() : "",
                            "oldStatus", oldStatus != null ? oldStatus.name() : "",
                            "newStatus", updated.getStatus() != null ? updated.getStatus().name() : ""
                    ))
                    .build());
        }
        return userMapper.toResponse(updated);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = findUser(id);
        UserStatus oldStatus = user.getStatus();
        if (user.getStatus() != status) {
            user.setStatus(status);
            revokeSessions(user);
        }
        User saved = userRepository.save(user);
        log.info("Cập nhật trạng thái người dùng id: {} sang: {}", id, status);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.USER)
                    .action(SystemLogAction.UPDATE_STATUS)
                    .resourceType("USER")
                    .resourceId(saved.getId())
                    .resourceCode(saved.getUsername())
                    .description("Chuyển trạng thái người dùng " + saved.getUsername() + " từ " + oldStatus + " sang " + status)
                    .detailMap(Map.of("oldStatus", oldStatus.name(), "newStatus", status.name()))
                    .build());
        }
        return userMapper.toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        String username = user.getUsername();
        revokeSessions(user);
        userRepository.saveAndFlush(user);
        userRepository.delete(user);
        log.info("Đã xóa mềm người dùng id: {}", id);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.USER)
                    .action(SystemLogAction.DELETE)
                    .resourceType("USER")
                    .resourceId(id)
                    .resourceCode(username)
                    .description("Xóa mềm tài khoản người dùng " + username)
                    .build());
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));
    }

    private void revokeSessions(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        refreshTokenService.revokeAllByUser(user.getId());
    }
}
