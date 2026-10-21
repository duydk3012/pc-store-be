package com.datn.pc_store.modules.category.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.category.dto.CategoryCreateRequest;
import com.datn.pc_store.modules.category.dto.CategoryResponse;
import com.datn.pc_store.modules.category.dto.CategoryTreeResponse;
import com.datn.pc_store.modules.category.dto.CategoryUpdateRequest;
import com.datn.pc_store.modules.category.entity.Category;
import com.datn.pc_store.modules.category.enums.CategoryStatus;
import com.datn.pc_store.modules.category.mapper.CategoryMapper;
import com.datn.pc_store.modules.category.repository.CategoryRepository;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category mockParent;
    private Category mockCategory;
    private CategoryResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockParent = Category.builder()
                .id(1L)
                .name("Linh kiện máy tính")
                .slug("linh-kien-may-tinh")
                .status(CategoryStatus.ACTIVE)
                .build();

        mockCategory = Category.builder()
                .id(2L)
                .name("Mainboard")
                .slug("mainboard")
                .parent(mockParent)
                .status(CategoryStatus.ACTIVE)
                .build();

        mockResponse = CategoryResponse.builder()
                .id(2L)
                .name("Mainboard")
                .slug("mainboard")
                .parentId(1L)
                .parentName("Linh kiện máy tính")
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Tests cho createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("Tạo mới thành công và tự động sinh slug từ tên")
        void createCategory_Success_AutoSlug() {
            CategoryCreateRequest req = CategoryCreateRequest.builder()
                    .name("Bo mạch chủ")
                    .parentId(1L)
                    .build();

            when(categoryRepository.existsBySlug("bo-mach-chu")).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(mockCategory);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockParent));
            when(categoryRepository.save(mockCategory)).thenReturn(mockCategory);
            when(categoryMapper.toResponse(mockCategory)).thenReturn(mockResponse);

            CategoryResponse result = categoryService.createCategory(req);

            assertThat(result).isNotNull();
            assertThat(mockCategory.getSlug()).isEqualTo("bo-mach-chu");
            verify(categoryRepository).save(mockCategory);
        }

        @Test
        @DisplayName("Ném ngoại lệ DuplicateResourceException khi slug đã tồn tại")
        void createCategory_DuplicateSlug_ThrowsException() {
            CategoryCreateRequest req = CategoryCreateRequest.builder()
                    .name("Mainboard")
                    .slug("mainboard")
                    .build();

            when(categoryRepository.existsBySlug("mainboard")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("slug");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném ngoại lệ ResourceNotFoundException khi parentId không tồn tại")
        void createCategory_ParentNotFound_ThrowsException() {
            CategoryCreateRequest req = CategoryCreateRequest.builder()
                    .name("Mainboard")
                    .parentId(999L)
                    .build();

            when(categoryRepository.existsBySlug("mainboard")).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(mockCategory);
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.createCategory(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("Tests cho updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Cập nhật danh mục thành công")
        void updateCategory_Success() {
            CategoryUpdateRequest req = CategoryUpdateRequest.builder()
                    .name("Mainboard Intel")
                    .slug("mainboard-intel")
                    .parentId(1L)
                    .build();

            when(categoryRepository.findById(2L)).thenReturn(Optional.of(mockCategory));
            when(categoryRepository.existsBySlugAndIdNot("mainboard-intel", 2L)).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockParent));
            when(categoryRepository.save(mockCategory)).thenReturn(mockCategory);
            when(categoryMapper.toResponse(mockCategory)).thenReturn(mockResponse);

            CategoryResponse result = categoryService.updateCategory(2L, req);

            assertThat(result).isNotNull();
            verify(categoryMapper).updateEntityFromDto(req, mockCategory);
            verify(categoryRepository).save(mockCategory);
        }

        @Test
        @DisplayName("Ném BadRequestException khi danh mục chọn chính nó làm cha")
        void updateCategory_SelfParent_ThrowsException() {
            CategoryUpdateRequest req = CategoryUpdateRequest.builder()
                    .name("Mainboard")
                    .parentId(2L)
                    .build();

            when(categoryRepository.findById(2L)).thenReturn(Optional.of(mockCategory));
            when(categoryRepository.existsBySlugAndIdNot("mainboard", 2L)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.updateCategory(2L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("chính nó");
        }
    }

    @Nested
    @DisplayName("Tests cho deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Xóa mềm danh mục thành công khi không có danh mục con")
        void deleteCategory_Success() {
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(mockCategory));
            when(categoryRepository.existsByParentId(2L)).thenReturn(false);

            categoryService.deleteCategory(2L);

            verify(categoryRepository).delete(mockCategory);
        }

        @Test
        @DisplayName("Ném BadRequestException khi danh mục đang có danh mục con")
        void deleteCategory_HasChildren_ThrowsException() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockParent));
            when(categoryRepository.existsByParentId(1L)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("danh mục con");

            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Tests cho getCategoryTree")
    class GetCategoryTreeTests {

        @Test
        @DisplayName("Lấy cây danh mục phân cấp thành công")
        void getCategoryTree_Success() {
            CategoryTreeResponse parentTree = CategoryTreeResponse.builder()
                    .id(1L)
                    .name("Linh kiện máy tính")
                    .slug("linh-kien-may-tinh")
                    .build();

            CategoryTreeResponse childTree = CategoryTreeResponse.builder()
                    .id(2L)
                    .name("Mainboard")
                    .slug("mainboard")
                    .parentId(1L)
                    .build();

            when(categoryRepository.findByParentIsNullAndStatus(CategoryStatus.ACTIVE))
                    .thenReturn(List.of(mockParent));
            when(categoryMapper.toTreeResponse(mockParent)).thenReturn(parentTree);
            when(categoryRepository.findByParentIdAndStatus(1L, CategoryStatus.ACTIVE))
                    .thenReturn(List.of(mockCategory));
            when(categoryMapper.toTreeResponse(mockCategory)).thenReturn(childTree);
            when(categoryRepository.findByParentIdAndStatus(2L, CategoryStatus.ACTIVE))
                    .thenReturn(List.of());

            List<CategoryTreeResponse> tree = categoryService.getCategoryTree(true);

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getChildren()).hasSize(1);
            assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("Mainboard");
        }
    }
}
