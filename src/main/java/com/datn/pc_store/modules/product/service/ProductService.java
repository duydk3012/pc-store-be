package com.datn.pc_store.product.service;

import com.datn.pc_store.product.dto.ProductRequest;
import com.datn.pc_store.product.dto.ProductResponse;
import com.datn.pc_store.product.entity.Product;
import com.datn.pc_store.product.mapper.ProductMapper;
import com.datn.pc_store.product.enums.ProductStatus;
import com.datn.pc_store.product.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProductService {

    private static final int ACTIVE = 0;
    private static final int DELETED = 1;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllByIsDeletedOrderByIdAsc(ACTIVE)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findActiveProduct(id));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ON_SALE);
        }
        product.setIsDeleted(ACTIVE);
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActiveProduct(id);
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.saveAndFlush(product));
    }

    public void delete(Long id) {
        Product product = findActiveProduct(id);
        product.setIsDeleted(DELETED);
        productRepository.save(product);
    }

    private Product findActiveProduct(Long id) {
        return productRepository.findByIdAndIsDeleted(id, ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

}
