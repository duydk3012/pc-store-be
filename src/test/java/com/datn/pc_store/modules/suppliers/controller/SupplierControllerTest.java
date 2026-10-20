package com.datn.pc_store.modules.suppliers.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.suppliers.dto.SupplierCreateRequest;
import com.datn.pc_store.modules.suppliers.dto.SupplierResponse;
import com.datn.pc_store.modules.suppliers.dto.SupplierStatusUpdateRequest;
import com.datn.pc_store.modules.suppliers.dto.SupplierUpdateRequest;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import com.datn.pc_store.modules.suppliers.service.SupplierService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierController supplierController;

    private SupplierResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supplierController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = SupplierResponse.builder()
                .id(1L)
                .code("NCC_ASUS")
                .name("Asus Vietnam")
                .email("contact@asus.vn")
                .phoneNumber("0901234567")
                .status(SupplierStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/suppliers - Trả về 200 OK với danh sách phân trang")
    void getAllSuppliers_Returns200() throws Exception {
        PageResponse<SupplierResponse> pageResponse = PageResponse.<SupplierResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(supplierService.getAllSuppliers(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/suppliers")
                        .param("keyword", "asus")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("NCC_ASUS"));
    }

    @Test
    @DisplayName("GET /api/admin/suppliers/{id} - Trả về 200 OK khi tìm thấy")
    void getSupplierById_Returns200() throws Exception {
        when(supplierService.getSupplierById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/admin/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("NCC_ASUS"));
    }

    @Test
    @DisplayName("GET /api/admin/suppliers/{id} - Trả về 404 NOT_FOUND khi không tồn tại")
    void getSupplierById_NotFound_Returns404() throws Exception {
        when(supplierService.getSupplierById(99L))
                .thenThrow(new ResourceNotFoundException("Nhà cung cấp", "id", 99L));

        mockMvc.perform(get("/api/admin/suppliers/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/admin/suppliers - Trả về 201 CREATED khi dữ liệu hợp lệ")
    void createSupplier_Success_Returns201() throws Exception {
        SupplierCreateRequest req = SupplierCreateRequest.builder()
                .code("NCC_ASUS")
                .name("Asus Vietnam")
                .email("contact@asus.vn")
                .phoneNumber("0901234567")
                .build();

        when(supplierService.createSupplier(any(SupplierCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/admin/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NCC_ASUS"));
    }

    @Test
    @DisplayName("POST /api/admin/suppliers - Trả về 400 BAD_REQUEST khi validation thất bại")
    void createSupplier_InvalidValidation_Returns400() throws Exception {
        SupplierCreateRequest invalidReq = SupplierCreateRequest.builder()
                .code("") // Blank -> vi phạm
                .name("") // Blank -> vi phạm
                .email("invalid-email") // Sai format email
                .build();

        mockMvc.perform(post("/api/admin/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/admin/suppliers - Trả về 409 CONFLICT khi trùng mã")
    void createSupplier_DuplicateCode_Returns409() throws Exception {
        SupplierCreateRequest req = SupplierCreateRequest.builder()
                .code("NCC_ASUS")
                .name("Asus Vietnam")
                .email("contact@asus.vn")
                .build();

        when(supplierService.createSupplier(any(SupplierCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Nhà cung cấp", "mã", "NCC_ASUS"));

        mockMvc.perform(post("/api/admin/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/admin/suppliers/{id} - Trả về 200 OK khi cập nhật thành công")
    void updateSupplier_Returns200() throws Exception {
        SupplierUpdateRequest req = SupplierUpdateRequest.builder()
                .code("NCC_ASUS")
                .name("Asus Vietnam Updated")
                .email("updated@asus.vn")
                .status(SupplierStatus.ACTIVE)
                .build();

        when(supplierService.updateSupplier(eq(1L), any(SupplierUpdateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/admin/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/admin/suppliers/{id}/status - Trả về 200 OK khi chuyển trạng thái")
    void updateStatus_Returns200() throws Exception {
        SupplierStatusUpdateRequest req = SupplierStatusUpdateRequest.builder()
                .status(SupplierStatus.INACTIVE)
                .build();

        sampleResponse.setStatus(SupplierStatus.INACTIVE);
        when(supplierService.updateStatus(1L, SupplierStatus.INACTIVE)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/admin/suppliers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("DELETE /api/admin/suppliers/{id} - Trả về 204 NO_CONTENT khi xóa mềm thành công")
    void deleteSupplier_Returns204() throws Exception {
        doNothing().when(supplierService).deleteSupplier(1L);

        mockMvc.perform(delete("/api/admin/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
