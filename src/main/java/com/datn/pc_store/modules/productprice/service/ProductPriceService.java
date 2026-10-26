package com.datn.pc_store.modules.productprice.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.*;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.productprice.dto.*;
import com.datn.pc_store.modules.productprice.entity.ProductPrice;
import com.datn.pc_store.modules.productprice.mapper.ProductPriceMapper;
import com.datn.pc_store.modules.productprice.repository.ProductPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductPriceService {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "productId", "price", "effectiveFrom", "effectiveTo", "presentPrice", "createdBy", "createdDate");
    private final ProductPriceRepository productPriceRepository;
    private final ProductRepository productRepository;
    private final ProductPriceMapper productPriceMapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductPriceResponse> getPrices(Long productId, Boolean presentPrice, Pageable pageable) {
        for (var order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + order.getProperty()
                        + ". Sử dụng sort=createdDate,desc hoặc bỏ tham số sort.");
            }
        }
        return PageResponse.of(productPriceRepository.searchPrices(productId, presentPrice, pageable).map(productPriceMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductPriceResponse getPriceById(Long id) {
        return productPriceMapper.toResponse(findPrice(id));
    }

    @Transactional(readOnly = true)
    public ProductPriceResponse getCurrentPrice(Long productId, boolean publicOnly) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));
        if (publicOnly && product.getStatus() == ProductStatus.INACTIVE) {
            throw new ResourceNotFoundException("Sản phẩm", "id", productId);
        }
        ProductPriceResponse current = getCurrentPrices(List.of(productId)).get(productId);
        if (current == null) throw new ResourceNotFoundException("Giá hiện tại", "productId", productId);
        return current;
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductPriceResponse> getCurrentPrices(Collection<Long> productIds) {
        if (productIds.isEmpty()) return Map.of();
        Map<Long, ProductPriceResponse> result = new HashMap<>();
        for (var price : productPriceRepository.findCurrentPrices(productIds, now())) {
            if (result.put(price.getProductId(), productPriceMapper.toResponse(price)) != null) {
                throw conflict("Sản phẩm đang có nhiều giá cùng hiệu lực. Vui lòng áp dụng một giá mới để chuẩn hóa dữ liệu.");
            }
        }
        return result;
    }

    /** Append a version through the product workflow; historical amounts are immutable. */
    @Transactional
    public void recordProductPrice(Long productId, long amount) {
        lockProduct(productId);
        applyPrice(productId, amount, now());
    }

    private ProductPriceResponse applyPrice(Long productId, long amount, Instant at) {
        List<ProductPrice> oldPrices = productPriceRepository.findByProductIdAndPresentPriceTrue(productId);
        for (var old : oldPrices) {
            if (old.getEffectiveFrom() != null && old.getEffectiveFrom().isAfter(at)) {
                throw conflict("Sản phẩm có bản giá tương lai; module hiện chỉ hỗ trợ đổi giá có hiệu lực ngay.");
            }
        }
        for (var old : oldPrices) {
            old.setPresentPrice(false);
            if (old.getEffectiveTo() == null || old.getEffectiveTo().isAfter(at)) old.setEffectiveTo(at);
        }
        productPriceRepository.saveAllAndFlush(oldPrices);
        ProductPrice price = ProductPrice.builder().productId(productId).price(amount)
                .effectiveFrom(at).presentPrice(true).build();
        return productPriceMapper.toResponse(productPriceRepository.saveAndFlush(price));
    }

    private ProductPrice findPrice(Long id) {
        return productPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giá sản phẩm", "id", id));
    }

    private Product lockProduct(Long id) {
        return productRepository.findForPriceUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private ResponseStatusException conflict(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }
}
