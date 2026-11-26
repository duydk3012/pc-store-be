package com.datn.pc_store.modules.user.controller;

import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.user.dto.UserResponse;
import com.datn.pc_store.modules.user.dto.UserRoleCountResponse;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/users/count-by-role - Thành công trả về danh sách đếm theo vai trò")
    void testCountUsersByRoleSuccess() throws Exception {
        List<UserRoleCountResponse> mockResponse = List.of(
                UserRoleCountResponse.builder().role(RoleType.ADMIN).label("Quản trị viên").count(2).build(),
                UserRoleCountResponse.builder().role(RoleType.MANAGER).label("Quản lý").count(3).build(),
                UserRoleCountResponse.builder().role(RoleType.CUSTOMER).label("Khách hàng").count(50).build(),
                UserRoleCountResponse.builder().role(RoleType.STAFF_WAREHOUSE).label("Nhân viên kho").count(4).build(),
                UserRoleCountResponse.builder().role(RoleType.STAFF_SALES).label("Nhân viên bán hàng").count(5).build(),
                UserRoleCountResponse.builder().role(RoleType.STAFF_TECH).label("Nhân viên kỹ thuật").count(1).build()
        );

        when(userService.countUsersByRole(isNull(), isNull())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/users/count-by-role")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].label").value("Quản trị viên"))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[2].role").value("CUSTOMER"))
                .andExpect(jsonPath("$[2].count").value(50));
    }

    @Test
    @DisplayName("GET /api/admin/users/count-by-role?status=ACTIVE&role=CUSTOMER - Lọc theo trạng thái và vai trò")
    void testCountUsersByRoleWithFilters() throws Exception {
        List<UserRoleCountResponse> mockResponse = List.of(
                UserRoleCountResponse.builder().role(RoleType.CUSTOMER).label("Khách hàng").count(45).build()
        );

        when(userService.countUsersByRole(eq(RoleType.CUSTOMER), eq(UserStatus.ACTIVE))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/users/count-by-role")
                        .param("role", "CUSTOMER")
                        .param("status", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("CUSTOMER"))
                .andExpect(jsonPath("$[0].count").value(45));
    }

    @Test
    @DisplayName("GET /api/admin/users/count - Thành công đếm tổng số lượng người dùng")
    void testCountUsersSuccess() throws Exception {
        when(userService.countUsers(isNull(), isNull())).thenReturn(65L);

        mockMvc.perform(get("/api/admin/users/count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("65"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - Vẫn hoạt động bình thường, không bị trùng lặp đường dẫn")
    void testGetUserByIdStillWorks() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .role(RoleType.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserById(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/admin/users/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));
    }
}
