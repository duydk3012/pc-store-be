package com.datn.pc_store.modules.category.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.category.dto.CategoryCreateRequest;
import com.datn.pc_store.modules.category.dto.CategoryResponse;
import com.datn.pc_store.modules.category.dto.CategoryStatusUpdateRequest;
import com.datn.pc_store.modules.category.dto.CategoryUpdateRequest;
import com.datn.pc_store.modules.category.enums.CategoryStatus;
import com.datn.pc_store.modules.category.service.CategoryService;
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
class CategoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = CategoryResponse.builder()
                .id(1L)
                .name("Linh kiện máy tính")
                .slug("linh-kien-may-tinh")
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/categories - Trả về 200 OK")
    void getAllCategories_Returns200() throws Exception {
        PageResponse<CategoryResponse> pageResponse = PageResponse.<CategoryResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(categoryService.getAllCategories(any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Linh kiện máy tính"));
    }

    @Test
    @DisplayName("POST /api/admin/categories - Trả về 201 CREATED khi dữ liệu hợp lệ")
    void createCategory_Returns201() throws Exception {
        CategoryCreateRequest req = CategoryCreateRequest.builder()
                .name("Linh kiện máy tính")
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Linh kiện máy tính"));
    }

    @Test
    @DisplayName("POST /api/admin/categories - Trả về 400 BAD_REQUEST khi tên rỗng")
    void createCategory_BlankName_Returns400() throws Exception {
        CategoryCreateRequest req = CategoryCreateRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("DELETE /api/admin/categories/{id} - Trả về 204 NO_CONTENT")
    void deleteCategory_Returns204() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
