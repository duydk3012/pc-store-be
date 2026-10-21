package com.datn.pc_store.modules.brand.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.brand.dto.BrandCreateRequest;
import com.datn.pc_store.modules.brand.dto.BrandResponse;
import com.datn.pc_store.modules.brand.dto.BrandUpdateRequest;
import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.enums.BrandStatus;
import com.datn.pc_store.modules.brand.mapper.BrandMapper;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @InjectMocks
    private BrandService brandService;

    private Brand mockBrand;
    private BrandResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockBrand = Brand.builder()
                .id(1L)
                .name("ASUS")
                .slug("asus")
                .description("Asus Corporation")
                .status(BrandStatus.ACTIVE)
                .build();

        mockResponse = BrandResponse.builder()
                .id(1L)
                .name("ASUS")
                .slug("asus")
                .description("Asus Corporation")
                .status(BrandStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Tests cho createBrand")
    class CreateBrandTests {

        @Test
        @DisplayName("Tạo mới thương hiệu thành công với slug tự sinh")
        void createBrand_Success() {
            BrandCreateRequest req = BrandCreateRequest.builder()
                    .name("Gigabyte Technology")
                    .build();

            Brand gigabyte = Brand.builder()
                    .id(2L)
                    .name("Gigabyte Technology")
                    .slug("gigabyte-technology")
                    .status(BrandStatus.ACTIVE)
                    .build();

            when(brandRepository.existsBySlug("gigabyte-technology")).thenReturn(false);
            when(brandMapper.toEntity(req)).thenReturn(gigabyte);
            when(brandRepository.save(gigabyte)).thenReturn(gigabyte);
            when(brandMapper.toResponse(gigabyte)).thenReturn(mockResponse);

            BrandResponse result = brandService.createBrand(req);

            assertThat(result).isNotNull();
            assertThat(gigabyte.getSlug()).isEqualTo("gigabyte-technology");
            verify(brandRepository).save(gigabyte);
        }

        @Test
        @DisplayName("Ném DuplicateResourceException khi slug đã tồn tại")
        void createBrand_DuplicateSlug_ThrowsException() {
            BrandCreateRequest req = BrandCreateRequest.builder()
                    .name("ASUS")
                    .slug("asus")
                    .build();

            when(brandRepository.existsBySlug("asus")).thenReturn(true);

            assertThatThrownBy(() -> brandService.createBrand(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("slug");

            verify(brandRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests cho updateBrand")
    class UpdateBrandTests {

        @Test
        @DisplayName("Cập nhật thương hiệu thành công")
        void updateBrand_Success() {
            BrandUpdateRequest req = BrandUpdateRequest.builder()
                    .name("ASUS ROG")
                    .slug("asus-rog")
                    .build();

            when(brandRepository.findById(1L)).thenReturn(Optional.of(mockBrand));
            when(brandRepository.existsBySlugAndIdNot("asus-rog", 1L)).thenReturn(false);
            when(brandRepository.save(mockBrand)).thenReturn(mockBrand);
            when(brandMapper.toResponse(mockBrand)).thenReturn(mockResponse);

            BrandResponse result = brandService.updateBrand(1L, req);

            assertThat(result).isNotNull();
            verify(brandMapper).updateEntityFromDto(req, mockBrand);
            verify(brandRepository).save(mockBrand);
        }

        @Test
        @DisplayName("Ném DuplicateResourceException khi cập nhật trùng slug của thương hiệu khác")
        void updateBrand_DuplicateSlug_ThrowsException() {
            BrandUpdateRequest req = BrandUpdateRequest.builder()
                    .name("MSI")
                    .slug("msi")
                    .build();

            when(brandRepository.findById(1L)).thenReturn(Optional.of(mockBrand));
            when(brandRepository.existsBySlugAndIdNot("msi", 1L)).thenReturn(true);

            assertThatThrownBy(() -> brandService.updateBrand(1L, req))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(brandRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests cho deleteBrand")
    class DeleteBrandTests {

        @Test
        @DisplayName("Xóa mềm thương hiệu thành công")
        void deleteBrand_Success() {
            when(brandRepository.findById(1L)).thenReturn(Optional.of(mockBrand));

            brandService.deleteBrand(1L);

            verify(brandRepository).delete(mockBrand);
        }
    }
}
