package com.datn.pc_store.modules.brand.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.modules.brand.dto.BrandCreateRequest;
import com.datn.pc_store.modules.brand.dto.BrandResponse;
import com.datn.pc_store.modules.brand.enums.BrandStatus;
import com.datn.pc_store.modules.brand.service.BrandService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BrandControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BrandService brandService;

    @InjectMocks
    private BrandController brandController;

    private BrandResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(brandController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = BrandResponse.builder()
                .id(1L)
                .name("ASUS")
                .slug("asus")
                .status(BrandStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/brands - Trả về 200 OK")
    void getAllBrands_Returns200() throws Exception {
        PageResponse<BrandResponse> pageResponse = PageResponse.<BrandResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(brandService.getAllBrands(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("ASUS"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/admin/brands - Trả về 201 CREATED khi dữ liệu hợp lệ")
    void createBrand_Returns201() throws Exception {
        BrandCreateRequest req = BrandCreateRequest.builder()
                .name("ASUS")
                .build();

        when(brandService.createBrand(any(BrandCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ASUS"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("DELETE /api/admin/brands/{id} - Trả về 204 NO_CONTENT")
    void deleteBrand_Returns204() throws Exception {
        doNothing().when(brandService).deleteBrand(1L);

        mockMvc.perform(delete("/api/admin/brands/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
