package com.datn.pc_store.modules.product.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.*;
import com.datn.pc_store.common.util.SlugUtils;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.category.repository.CategoryRepository;
import com.datn.pc_store.modules.product.dto.*;
import com.datn.pc_store.modules.product.entity.*;
import com.datn.pc_store.modules.product.enums.*;
import com.datn.pc_store.modules.product.mapper.ProductMapper;
import com.datn.pc_store.modules.product.repository.*;
import com.datn.pc_store.modules.productprice.dto.ProductPriceResponse;
import com.datn.pc_store.modules.productprice.service.ProductPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "code", "slug", "price", "productType", "status", "brandId",
            "createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy"
    );

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductPriceService productPriceService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(String keyword, ProductStatus status, ProductType productType,
                                                        Long brandId, Long categoryId, boolean publicOnly, Pageable pageable) {
        for (var order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + order.getProperty()
                        + ". Sử dụng sort=createdDate,desc hoặc bỏ tham số sort để dùng mặc định.");
            }
        }
        var page = productRepository.searchProducts(keyword, status, productType, brandId, categoryId, publicOnly, pageable);
        Map<Long, List<ProductCategory>> links = loadCategoryLinks(page.getContent());
        Map<Long, ProductPriceResponse> prices = productPriceService.getCurrentPrices(page.getContent().stream().map(Product::getId).toList());
        return PageResponse.of(page.map(product -> toResponse(product, links.getOrDefault(product.getId(), List.of()), prices.get(product.getId()))));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id, boolean publicOnly) {
        Product product = findProduct(id);
        requireVisible(product, publicOnly);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug, boolean publicOnly) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "slug", slug));
        requireVisible(product, publicOnly);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest req) {
        String slug = prepareSlug(req.getName(), req.getSlug());
        String code = req.getCode().trim();
        validateUnique(code, slug, null);
        validateReferences(req.getBrandId(), req.getCategoryIds(), req.getPrimaryCategoryId());

        Product product = productMapper.toEntity(req);
        product.setName(req.getName().trim());
        product.setCode(code);
        product.setSlug(slug);
        if (req.getPrice() != null) product.setPrice(req.getPrice().longValueExact());
        product.setStatus(req.getStatus() == null ? ProductStatus.ACTIVE : req.getStatus());
        product.setProductType(req.getProductType() == null ? ProductType.OTHER : req.getProductType());
        Product saved = productRepository.save(product);
        replaceCategories(saved.getId(), req.getCategoryIds(), req.getPrimaryCategoryId());
        recordPrice(saved);
        log.info("Đã tạo sản phẩm id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest req) {
        Product product = lockProduct(id);
        String slug = prepareSlug(req.getName(), req.getSlug());
        String code = req.getCode().trim();
        validateUnique(code, slug, id);
        validateReferences(req.getBrandId(), req.getCategoryIds(), req.getPrimaryCategoryId());

        Long previousPrice = product.getPrice();
        productMapper.updateEntityFromDto(req, product);
        if (req.getPrice() != null) product.setPrice(req.getPrice().longValueExact());
        product.setName(req.getName().trim());
        product.setCode(code);
        product.setSlug(slug);
        if (req.getStatus() != null) product.setStatus(req.getStatus());
        if (req.getProductType() != null) product.setProductType(req.getProductType());
        Product saved = productRepository.save(product);
        replaceCategories(id, req.getCategoryIds(), req.getPrimaryCategoryId());
        if (!Objects.equals(previousPrice, saved.getPrice())) recordPrice(saved);
        log.info("Đã cập nhật sản phẩm id: {}", id);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatus status) {
        Product product = lockProduct(id);
        product.setStatus(status);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(lockProduct(id));
        log.info("Đã xóa mềm sản phẩm id: {}", id);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
    }

    private Product lockProduct(Long id) {
        return productRepository.findForPriceUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
    }

    private void recordPrice(Product product) {
        if (product.getPrice() != null) {
            productPriceService.recordProductPrice(product.getId(), product.getPrice());
        }
    }

    private void requireVisible(Product product, boolean publicOnly) {
        if (publicOnly && product.getStatus() == ProductStatus.INACTIVE) {
            throw new ResourceNotFoundException("Sản phẩm", "id", product.getId());
        }
    }

    private String prepareSlug(String name, String requestedSlug) {
        String slug = SlugUtils.toSlug(requestedSlug == null || requestedSlug.isBlank() ? name : requestedSlug);
        if (slug.isBlank() || slug.length() > 255) {
            throw new BadRequestException("Slug sản phẩm phải có từ 1 đến 255 ký tự sau khi chuẩn hóa.");
        }
        return slug;
    }

    private void validateUnique(String code, String slug, Long id) {
        if (productRepository.countByCodeIncludingDeleted(code, id) > 0) {
            throw new DuplicateResourceException("Sản phẩm", "code", code);
        }
        if (productRepository.countBySlugIncludingDeleted(slug, id) > 0) {
            throw new DuplicateResourceException("Sản phẩm", "slug", slug);
        }
    }

    private void validateReferences(Long brandId, Set<Long> categoryIds, Long primaryCategoryId) {
        if (brandId != null && !brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException("Thương hiệu", "id", brandId);
        }
        Set<Long> ids = categoryIds == null ? Set.of() : categoryIds;
        if (primaryCategoryId != null && !ids.contains(primaryCategoryId)) {
            throw new BadRequestException("Danh mục chính phải nằm trong danh sách danh mục của sản phẩm.");
        }
        Set<Long> found = categoryRepository.findAllById(ids).stream().map(c -> c.getId()).collect(Collectors.toSet());
        for (Long id : ids) {
            if (!found.contains(id)) throw new ResourceNotFoundException("Danh mục", "id", id);
        }
    }

    private void replaceCategories(Long productId, Set<Long> categoryIds, Long primaryCategoryId) {
        productCategoryRepository.deleteLinksByProductId(productId);
        if (categoryIds == null || categoryIds.isEmpty()) return;
        productCategoryRepository.saveAll(categoryIds.stream().map(categoryId -> ProductCategory.builder()
                .productId(productId).categoryId(categoryId).primary(Objects.equals(categoryId, primaryCategoryId)).build()).toList());
    }

    private Map<Long, List<ProductCategory>> loadCategoryLinks(List<Product> products) {
        if (products.isEmpty()) return Map.of();
        return productCategoryRepository.findByProductIdIn(products.stream().map(Product::getId).toList())
                .stream().collect(Collectors.groupingBy(ProductCategory::getProductId));
    }

    private ProductResponse toResponse(Product product) {
        return toResponse(product, productCategoryRepository.findByProductIdIn(List.of(product.getId())),
                productPriceService.getCurrentPrices(List.of(product.getId())).get(product.getId()));
    }

    private ProductResponse toResponse(Product product, List<ProductCategory> links, ProductPriceResponse price) {
        ProductResponse response = productMapper.toResponse(product);
        response.setCategoryIds(links.stream().map(ProductCategory::getCategoryId).collect(Collectors.toCollection(TreeSet::new)));
        response.setPrimaryCategoryId(links.stream().filter(ProductCategory::isPrimary)
                .map(ProductCategory::getCategoryId).findFirst().orElse(null));
        response.setPriceId(price == null ? null : price.getId());
        return response;
    }
}
