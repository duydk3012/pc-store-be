package com.datn.pc_store.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datn.pc_store.product.dto.ProductRequest;
import com.datn.pc_store.product.entity.Product;
import com.datn.pc_store.product.enums.ProductStatus;
import com.datn.pc_store.product.mapper.ProductMapper;
import com.datn.pc_store.product.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.server.ResponseStatusException;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductService(
                productRepository, Mappers.getMapper(ProductMapper.class));
    }

    @Test
    void createTrimsRequiredFieldsAndMarksProductActive() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductRequest request = new ProductRequest(
                "  Gaming PC  ", "  PC-001  ", "  chiếc  ", null, 0,
                "Máy tính chơi game", "gaming-pc", "pc.jpg", 7L);

        var response = productService.create(request);

        assertEquals("Gaming PC", response.name());
        assertEquals("PC-001", response.sku());
        assertEquals("chiếc", response.unit());
        assertEquals(ProductStatus.ON_SALE, response.status());
        assertEquals(0, response.trackingType());
        assertEquals(0, response.isDeleted());
        assertEquals(7L, response.categoryBrandsId());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateChangesProductFields() {
        Product product = new Product();
        product.setId(42L);
        product.setIsDeleted(0);
        when(productRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        var response = productService.update(42L, new ProductRequest(
                "Gaming PC", "PC-001", "chiếc", ProductStatus.OUT_OF_STOCK, 0,
                null, "gaming-pc", null, 7L));

        assertEquals("Gaming PC", response.name());
        assertEquals("PC-001", response.sku());
        assertEquals("gaming-pc", response.slug());
        assertEquals(ProductStatus.OUT_OF_STOCK, response.status());
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void updateKeepsExistingStatusWhenStatusIsOmitted() {
        Product product = new Product();
        product.setId(42L);
        product.setIsDeleted(0);
        product.setStatus(ProductStatus.DISCONTINUED);
        when(productRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        var response = productService.update(42L, new ProductRequest(
                "Gaming PC", "PC-001", "chiếc", null, 0,
                null, "gaming-pc", null, 7L));

        assertEquals(ProductStatus.DISCONTINUED, response.status());
    }

    @Test
    void deleteSoftDeletesProduct() {
        Product product = new Product();
        product.setId(42L);
        product.setIsDeleted(0);
        when(productRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        productService.delete(42L);

        assertEquals(1, product.getIsDeleted());
        verify(productRepository).save(product);
    }

    @Test
    void getByIdReturnsNotFoundForMissingOrDeletedProduct() {
        when(productRepository.findByIdAndIsDeleted(42L, 0)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getById(42L));

        assertEquals(404, exception.getStatusCode().value());
    }
}
