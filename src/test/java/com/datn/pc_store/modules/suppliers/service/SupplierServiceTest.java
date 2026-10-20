package com.datn.pc_store.modules.suppliers.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.suppliers.dto.SupplierCreateRequest;
import com.datn.pc_store.modules.suppliers.dto.SupplierResponse;
import com.datn.pc_store.modules.suppliers.dto.SupplierUpdateRequest;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import com.datn.pc_store.modules.suppliers.mapper.SupplierMapper;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
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
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier mockSupplier;
    private SupplierResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockSupplier = Supplier.builder()
                .id(1L)
                .code("NCC_ASUS")
                .name("Asus Vietnam")
                .contactPerson("Nguyen Van A")
                .email("contact@asus.vn")
                .phoneNumber("0901234567")
                .address("TP.HCM")
                .taxCode("0312345678")
                .status(SupplierStatus.ACTIVE)
                .build();

        mockResponse = SupplierResponse.builder()
                .id(1L)
                .code("NCC_ASUS")
                .name("Asus Vietnam")
                .contactPerson("Nguyen Van A")
                .email("contact@asus.vn")
                .phoneNumber("0901234567")
                .address("TP.HCM")
                .taxCode("0312345678")
                .status(SupplierStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Tests cho createSupplier")
    class CreateSupplierTests {

        @Test
        @DisplayName("Tạo mới thành công khi dữ liệu hợp lệ")
        void createSupplier_Success() {
            SupplierCreateRequest req = SupplierCreateRequest.builder()
                    .code("NCC_ASUS")
                    .name("Asus Vietnam")
                    .email("contact@asus.vn")
                    .phoneNumber("0901234567")
                    .build();

            when(supplierRepository.existsByCode(req.getCode())).thenReturn(false);
            when(supplierRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(supplierMapper.toEntity(req)).thenReturn(mockSupplier);
            when(supplierRepository.save(mockSupplier)).thenReturn(mockSupplier);
            when(supplierMapper.toResponse(mockSupplier)).thenReturn(mockResponse);

            SupplierResponse result = supplierService.createSupplier(req);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("NCC_ASUS");
            verify(supplierRepository).save(mockSupplier);
        }

        @Test
        @DisplayName("Ném ngoại lệ DuplicateResourceException khi mã code đã tồn tại")
        void createSupplier_DuplicateCode_ThrowsException() {
            SupplierCreateRequest req = SupplierCreateRequest.builder()
                    .code("NCC_ASUS")
                    .name("Asus Vietnam")
                    .email("contact@asus.vn")
                    .build();

            when(supplierRepository.existsByCode(req.getCode())).thenReturn(true);

            assertThatThrownBy(() -> supplierService.createSupplier(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("mã");

            verify(supplierRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném ngoại lệ DuplicateResourceException khi email đã tồn tại")
        void createSupplier_DuplicateEmail_ThrowsException() {
            SupplierCreateRequest req = SupplierCreateRequest.builder()
                    .code("NCC_NEW")
                    .name("Asus Vietnam")
                    .email("contact@asus.vn")
                    .build();

            when(supplierRepository.existsByCode(req.getCode())).thenReturn(false);
            when(supplierRepository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> supplierService.createSupplier(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(supplierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests cho getSupplierById và getSupplierByCode")
    class GetSupplierTests {

        @Test
        @DisplayName("Lấy chi tiết theo ID thành công")
        void getSupplierById_Success() {
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(mockSupplier));
            when(supplierMapper.toResponse(mockSupplier)).thenReturn(mockResponse);

            SupplierResponse result = supplierService.getSupplierById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Ném ngoại lệ ResourceNotFoundException khi không tìm thấy ID")
        void getSupplierById_NotFound_ThrowsException() {
            when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Lấy chi tiết theo Code thành công")
        void getSupplierByCode_Success() {
            when(supplierRepository.findByCode("NCC_ASUS")).thenReturn(Optional.of(mockSupplier));
            when(supplierMapper.toResponse(mockSupplier)).thenReturn(mockResponse);

            SupplierResponse result = supplierService.getSupplierByCode("NCC_ASUS");

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("NCC_ASUS");
        }

        @Test
        @DisplayName("Ném ngoại lệ ResourceNotFoundException khi không tìm thấy Code")
        void getSupplierByCode_NotFound_ThrowsException() {
            when(supplierRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.getSupplierByCode("UNKNOWN"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");
        }
    }

    @Nested
    @DisplayName("Tests cho updateSupplier")
    class UpdateSupplierTests {

        @Test
        @DisplayName("Cập nhật thông tin thành công")
        void updateSupplier_Success() {
            SupplierUpdateRequest req = SupplierUpdateRequest.builder()
                    .code("NCC_ASUS_NEW")
                    .name("Asus VN New")
                    .email("new@asus.vn")
                    .status(SupplierStatus.ACTIVE)
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(mockSupplier));
            when(supplierRepository.existsByCodeAndIdNot(req.getCode(), 1L)).thenReturn(false);
            when(supplierRepository.existsByEmailAndIdNot(req.getEmail(), 1L)).thenReturn(false);
            when(supplierRepository.save(mockSupplier)).thenReturn(mockSupplier);
            when(supplierMapper.toResponse(mockSupplier)).thenReturn(mockResponse);

            SupplierResponse result = supplierService.updateSupplier(1L, req);

            assertThat(result).isNotNull();
            verify(supplierMapper).updateEntityFromDto(req, mockSupplier);
            verify(supplierRepository).save(mockSupplier);
        }

        @Test
        @DisplayName("Cập nhật trùng mã code với nhà cung cấp khác")
        void updateSupplier_DuplicateCode_ThrowsException() {
            SupplierUpdateRequest req = SupplierUpdateRequest.builder()
                    .code("NCC_EXISTING")
                    .name("Asus VN")
                    .email("contact@asus.vn")
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(mockSupplier));
            when(supplierRepository.existsByCodeAndIdNot(req.getCode(), 1L)).thenReturn(true);

            assertThatThrownBy(() -> supplierService.updateSupplier(1L, req))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(supplierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests cho updateStatus và deleteSupplier")
    class StatusAndDeleteTests {

        @Test
        @DisplayName("Chuyển trạng thái thành công")
        void updateStatus_Success() {
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(mockSupplier));
            when(supplierRepository.save(mockSupplier)).thenReturn(mockSupplier);
            when(supplierMapper.toResponse(mockSupplier)).thenReturn(mockResponse);

            SupplierResponse result = supplierService.updateStatus(1L, SupplierStatus.INACTIVE);

            assertThat(result).isNotNull();
            assertThat(mockSupplier.getStatus()).isEqualTo(SupplierStatus.INACTIVE);
            verify(supplierRepository).save(mockSupplier);
        }

        @Test
        @DisplayName("Xóa mềm thành công")
        void deleteSupplier_Success() {
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(mockSupplier));

            supplierService.deleteSupplier(1L);

            verify(supplierRepository).delete(mockSupplier);
        }
    }

    @Nested
    @DisplayName("Tests cho getAllSuppliers")
    class GetAllSuppliersTests {

        @Test
        @DisplayName("Lấy danh sách phân trang thành công")
        void getAllSuppliers_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Supplier> page = new PageImpl<>(List.of(mockSupplier), pageable, 1);

            when(supplierRepository.searchSuppliers("asus", SupplierStatus.ACTIVE, pageable)).thenReturn(page);
            when(supplierMapper.toResponse(mockSupplier)).thenReturn(mockResponse);

            PageResponse<SupplierResponse> response = supplierService.getAllSuppliers("asus", SupplierStatus.ACTIVE, pageable);

            assertThat(response).isNotNull();
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent()).hasSize(1);
        }
    }
}
