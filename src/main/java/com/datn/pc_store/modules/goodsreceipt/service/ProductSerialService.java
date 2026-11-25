package com.datn.pc_store.modules.goodsreceipt.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.goodsreceipt.dto.ProductSerialResponse;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.mapper.GoodsReceiptMapper;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductSerialService {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "serialNumber", "status", "createdDate", "lastModifiedDate"
    );

    private final ProductSerialRepository productSerialRepository;
    private final GoodsReceiptMapper goodsReceiptMapper;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public ProductSerialResponse getBySerialNumber(String serialNumber) {
        return goodsReceiptMapper.toSerialResponse(
                productSerialRepository.findFirstBySerialNumberAndIsTerminatedFalse(serialNumber)
                        .or(() -> productSerialRepository.findFirstBySerialNumberOrderByCreatedDateDesc(serialNumber))
                        .orElseThrow(() -> new ResourceNotFoundException("Serial sản phẩm", "serialNumber", serialNumber))
        );
    }

    @Transactional(readOnly = true)
    public ProductSerialResponse getById(Long id) {
        return goodsReceiptMapper.toSerialResponse(productSerialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serial sản phẩm", "id", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSerialResponse> getActiveSerialsByProduct(Long productId, ProductSerialStatus status, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm", "id", productId);
        }
        for (var sort : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(sort.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + sort.getProperty()
                        + ". Sử dụng sort=createdDate,desc hoặc bỏ tham số sort để dùng mặc định.");
            }
        }
        Page<com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial> page =
                productSerialRepository.findActiveSerialsByProduct(productId, status, pageable);
        return PageResponse.of(page.map(goodsReceiptMapper::toSerialResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSerialResponse> searchSerials(
            Long supplierId,
            Long productId,
            ProductSerialStatus status,
            String keyword,
            Pageable pageable
    ) {
        if (supplierId != null && !supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("Nhà cung cấp", "id", supplierId);
        }
        if (productId != null && !productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm", "id", productId);
        }
        for (var sort : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(sort.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + sort.getProperty()
                        + ". Sử dụng sort=createdDate,desc hoặc bỏ tham số sort để dùng mặc định.");
            }
        }
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Page<ProductSerial> page = productSerialRepository.searchSerials(
                supplierId, productId, status, cleanKeyword, pageable
        );
        return PageResponse.of(page.map(goodsReceiptMapper::toSerialResponse));
    }

    private static final Set<ProductSerialStatus> WAREHOUSE_EDITABLE_STATUSES = Set.of(
            ProductSerialStatus.GOOD_CONDITION,
            ProductSerialStatus.DEFECTIVE,
            ProductSerialStatus.IN_STOCK,
            ProductSerialStatus.PENDING_RETURN
    );

    @Transactional
    public ProductSerialResponse updateStatus(Long id, ProductSerialStatus newStatus) {
        ProductSerial ps = productSerialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serial sản phẩm", "id", id));
        validateManualStatusChange(ps, newStatus);
        ps.setStatus(newStatus);
        ProductSerial saved = productSerialRepository.save(ps);
        return goodsReceiptMapper.toSerialResponse(saved);
    }

    @Transactional
    public List<ProductSerialResponse> bulkUpdateStatus(List<String> serialNumbers, ProductSerialStatus newStatus) {
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            return List.of();
        }
        Set<String> cleanNumbers = serialNumbers.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
        if (cleanNumbers.isEmpty()) {
            return List.of();
        }
        List<ProductSerial> serials = productSerialRepository.lockActiveBySerialNumbers(cleanNumbers);
        for (ProductSerial ps : serials) {
            validateManualStatusChange(ps, newStatus);
            ps.setStatus(newStatus);
        }
        List<ProductSerial> saved = productSerialRepository.saveAll(serials);
        return saved.stream().map(goodsReceiptMapper::toSerialResponse).toList();
    }

    private void validateManualStatusChange(ProductSerial ps, ProductSerialStatus newStatus) {
        if (ps.isTerminated()) {
            throw new BadRequestException("Serial '" + ps.getSerialNumber() + "' đã kết thúc, không thể thay đổi trạng thái.");
        }
        if (!WAREHOUSE_EDITABLE_STATUSES.contains(ps.getStatus())) {
            throw new BadRequestException("Serial '" + ps.getSerialNumber() + "' đang thuộc quy trình "
                    + ps.getStatus() + ", không thể cập nhật thủ công.");
        }
        if (!WAREHOUSE_EDITABLE_STATUSES.contains(newStatus)) {
            throw new BadRequestException("Không thể chuyển thủ công serial sang trạng thái quy trình: " + newStatus);
        }
    }
}
