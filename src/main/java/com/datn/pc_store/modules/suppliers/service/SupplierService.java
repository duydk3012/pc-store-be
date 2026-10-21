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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getAllSuppliers(String keyword, SupplierStatus status, Pageable pageable) {
        log.info("Lấy danh sách nhà cung cấp với keyword: '{}', status: '{}', pageable: {}", keyword, status, pageable);
        Page<Supplier> page = supplierRepository.searchSuppliers(keyword, status, pageable);
        return PageResponse.of(page.map(supplierMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        log.info("Lấy chi tiết nhà cung cấp id: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "id", id));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByCode(String code) {
        log.info("Lấy chi tiết nhà cung cấp theo mã: {}", code);
        Supplier supplier = supplierRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "mã", code));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierCreateRequest req) {
        log.info("Tạo mới nhà cung cấp với mã: {}", req.getCode());

        if (supplierRepository.existsByCode(req.getCode())) {
            throw new DuplicateResourceException("Nhà cung cấp", "mã", req.getCode());
        }
        if (supplierRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Nhà cung cấp", "email", req.getEmail());
        }

        Supplier supplier = supplierMapper.toEntity(req);
        if (supplier.getStatus() == null) {
            supplier.setStatus(SupplierStatus.ACTIVE);
        }

        Supplier saved = supplierRepository.save(supplier);
        log.info("Tạo mới nhà cung cấp thành công với id: {}", saved.getId());
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierUpdateRequest req) {
        log.info("Cập nhật thông tin nhà cung cấp id: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "id", id));

        if (supplierRepository.existsByCodeAndIdNot(req.getCode(), id)) {
            throw new DuplicateResourceException("Nhà cung cấp", "mã", req.getCode());
        }
        if (supplierRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
            throw new DuplicateResourceException("Nhà cung cấp", "email", req.getEmail());
        }

        supplierMapper.updateEntityFromDto(req, supplier);
        if (req.getStatus() != null) {
            supplier.setStatus(req.getStatus());
        }

        Supplier updated = supplierRepository.save(supplier);
        log.info("Cập nhật nhà cung cấp id: {} thành công", updated.getId());
        return supplierMapper.toResponse(updated);
    }

    @Transactional
    public SupplierResponse updateStatus(Long id, SupplierStatus status) {
        log.info("Chuyển trạng thái nhà cung cấp id: {} sang: {}", id, status);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "id", id));

        supplier.setStatus(status);
        Supplier updated = supplierRepository.save(supplier);
        log.info("Chuyển trạng thái nhà cung cấp id: {} thành công", updated.getId());
        return supplierMapper.toResponse(updated);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        log.info("Yêu cầu xóa mềm nhà cung cấp id: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "id", id));

        // Soft delete bằng cách gọi delete (được kích hoạt thông qua @SQLDelete trên Entity)
        supplierRepository.delete(supplier);
        log.info("Đã xóa mềm nhà cung cấp id: {} thành công", id);
    }
}
