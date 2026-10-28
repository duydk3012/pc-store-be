package com.datn.pc_store.modules.goodsreceipt.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.goodsreceipt.dto.ProductSerialResponse;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.mapper.GoodsReceiptMapper;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.product.repository.ProductRepository;
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
}
