package com.datn.pc_store.modules.goodsreceipt.service;

import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.goodsreceipt.dto.ProductSerialResponse;
import com.datn.pc_store.modules.goodsreceipt.mapper.GoodsReceiptMapper;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSerialService {
    private final ProductSerialRepository productSerialRepository;
    private final GoodsReceiptMapper goodsReceiptMapper;

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
    public List<ProductSerialResponse> getActiveSerialsByProduct(Long productId) {
        return productSerialRepository.findAllByProductIdAndIsTerminatedFalse(productId).stream()
                .map(goodsReceiptMapper::toSerialResponse)
                .toList();
    }
}
