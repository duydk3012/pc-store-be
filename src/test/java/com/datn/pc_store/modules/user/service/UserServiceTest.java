package com.datn.pc_store.modules.user.service;

import com.datn.pc_store.modules.user.dto.UserRoleCountResponse;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("countUsersByRole - Trả về đầy đủ tất cả vai trò, các vai trò không có dữ liệu có count = 0")
    void testCountUsersByRoleAllRoles() {
        List<Object[]> repoResults = new ArrayList<>();
        repoResults.add(new Object[]{RoleType.ADMIN, 2L});
        repoResults.add(new Object[]{RoleType.CUSTOMER, 15L});

        when(userRepository.countUsersByRoleGrouped(null, null)).thenReturn(repoResults);

        List<UserRoleCountResponse> responses = userService.countUsersByRole(null, null);

        assertThat(responses).hasSize(RoleType.values().length);

        UserRoleCountResponse adminResponse = responses.stream()
                .filter(r -> r.getRole() == RoleType.ADMIN)
                .findFirst().orElseThrow();
        assertThat(adminResponse.getCount()).isEqualTo(2L);
        assertThat(adminResponse.getLabel()).isEqualTo("Quản trị viên");

        UserRoleCountResponse customerResponse = responses.stream()
                .filter(r -> r.getRole() == RoleType.CUSTOMER)
                .findFirst().orElseThrow();
        assertThat(customerResponse.getCount()).isEqualTo(15L);
        assertThat(customerResponse.getLabel()).isEqualTo("Khách hàng");

        UserRoleCountResponse techResponse = responses.stream()
                .filter(r -> r.getRole() == RoleType.STAFF_TECH)
                .findFirst().orElseThrow();
        assertThat(techResponse.getCount()).isEqualTo(0L);
        assertThat(techResponse.getLabel()).isEqualTo("Nhân viên kỹ thuật");
    }

    @Test
    @DisplayName("countUsersByRole - Xử lý khi kết quả trả về từ DB là kiểu Number (database code)")
    void testCountUsersByRoleWithNumberCode() {
        List<Object[]> repoResults = new ArrayList<>();
        // RoleType.MANAGER có value = 1
        repoResults.add(new Object[]{(byte) 1, 5L});

        when(userRepository.countUsersByRoleGrouped(null, UserStatus.ACTIVE)).thenReturn(repoResults);

        List<UserRoleCountResponse> responses = userService.countUsersByRole(null, UserStatus.ACTIVE);

        UserRoleCountResponse managerResponse = responses.stream()
                .filter(r -> r.getRole() == RoleType.MANAGER)
                .findFirst().orElseThrow();
        assertThat(managerResponse.getCount()).isEqualTo(5L);
        assertThat(managerResponse.getLabel()).isEqualTo("Quản lý");
    }

    @Test
    @DisplayName("countUsersByRole - Khi chỉ lọc 1 vai trò cụ thể thì chỉ trả về vai trò đó")
    void testCountUsersByRoleFilteredSingleRole() {
        List<Object[]> repoResults = new ArrayList<>();
        repoResults.add(new Object[]{RoleType.CUSTOMER, 20L});

        when(userRepository.countUsersByRoleGrouped(RoleType.CUSTOMER, UserStatus.ACTIVE)).thenReturn(repoResults);

        List<UserRoleCountResponse> responses = userService.countUsersByRole(RoleType.CUSTOMER, UserStatus.ACTIVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRole()).isEqualTo(RoleType.CUSTOMER);
        assertThat(responses.get(0).getCount()).isEqualTo(20L);
    }

    @Test
    @DisplayName("countUsers - Đếm tổng số lượng người dùng qua repository")
    void testCountUsers() {
        when(userRepository.countUsers(RoleType.CUSTOMER, UserStatus.ACTIVE)).thenReturn(30L);

        long count = userService.countUsers(RoleType.CUSTOMER, UserStatus.ACTIVE);
        assertThat(count).isEqualTo(30L);
    }
}
