package com.datn.pc_store.product.service;

import com.datn.pc_store.product.dto.ProductRequest;
import com.datn.pc_store.product.dto.ProductResponse;
import com.datn.pc_store.product.entity.Product;
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

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllByIsDeletedOrderByIdAsc(ACTIVE)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findActiveProduct(id));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        product.setStatus(request.status() == null ? ProductStatus.ON_SALE : request.status());
        product.setIsDeleted(ACTIVE);
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActiveProduct(id);
        applyRequest(product, request);
        return ProductResponse.from(productRepository.saveAndFlush(product));
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

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setSku(request.sku().trim());
        product.setUnit(request.unit().trim());
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        product.setTrackingType(request.trackingType());
        product.setDescription(request.description());
        product.setSlug(request.slug());
        product.setImageUrl(request.imageUrl());
        product.setCategoryBrandsId(request.categoryBrandsId());
    }
}
