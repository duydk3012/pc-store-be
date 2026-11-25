package com.datn.pc_store.modules.product.repository;

import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id=:id")
    Optional<Product> findForUpdate(@Param("id") Long id);

    Optional<Product> findBySlug(String slug);

    // Include soft-deleted products: their unique code and slug remain reserved.
    @Query(value = "SELECT COUNT(*) FROM products WHERE code = :code AND (:id IS NULL OR id <> :id)", nativeQuery = true)
    long countByCodeIncludingDeleted(@Param("code") String code, @Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM products WHERE slug = :slug AND (:id IS NULL OR id <> :id)", nativeQuery = true)
    long countBySlugIncludingDeleted(@Param("slug") String slug, @Param("id") Long id);

    @Query("""
        SELECT p FROM Product p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:publicOnly = false OR p.status <> com.datn.pc_store.modules.product.enums.ProductStatus.INACTIVE)
          AND (:productType IS NULL OR p.productType = :productType)
          AND (:brandId IS NULL OR p.brandId = :brandId)
          AND (:categoryId IS NULL OR EXISTS
               (SELECT pc.id FROM ProductCategory pc WHERE pc.productId = p.id AND pc.categoryId = :categoryId))
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status,
            @Param("productType") ProductType productType,
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("publicOnly") boolean publicOnly,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial ps ON ps.productId = p.id
        WHERE p.id IN :productIds
          AND p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
          AND ps.status = com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus.IN_STOCK
          AND ps.isTerminated = false
    """)
    java.util.List<Product> findAvailableInStockProductsByIds(@Param("productIds") java.util.Collection<Long> productIds);

    @Query("""
        SELECT p.id FROM Product p
        WHERE p.productType = com.datn.pc_store.modules.product.enums.ProductType.OTHER
          AND p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
          AND p.id <> :productId
        ORDER BY p.id ASC
    """)
    java.util.List<Long> findCompatibleAccessoryIds(@Param("productId") Long productId, Pageable pageable);

    @Query("""
        SELECT p.id FROM Product p
        WHERE p.productType = com.datn.pc_store.modules.product.enums.ProductType.OTHER
          AND p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
        ORDER BY p.id DESC
    """)
    java.util.List<Long> findTopSellingAccessoryIds(Pageable pageable);

    @Query("""
        SELECT p.id FROM Product p
        WHERE p.id <> :currentProductId
          AND p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
          AND EXISTS (
              SELECT 1 FROM ProductCategory pc1
              JOIN ProductCategory pc2 ON pc1.categoryId = pc2.categoryId
              WHERE pc1.productId = :currentProductId
                AND pc2.productId = p.id
          )
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        ORDER BY p.price ASC
    """)
    java.util.List<Long> findIdsByCategoryAndPriceRange(
            @Param("currentProductId") Long currentProductId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable);

    @Query("""
        SELECT p.id FROM Product p
        WHERE p.brandId = :brandId
          AND p.id <> :currentProductId
          AND p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
        ORDER BY p.id DESC
    """)
    java.util.List<Long> findTopSellingLaptopIdsByBrand(
            @Param("brandId") Long brandId,
            @Param("currentProductId") Long currentProductId,
            Pageable pageable);

    @Query("""
        SELECT p.id FROM Product p
        WHERE p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
        ORDER BY p.id ASC
    """)
    java.util.List<Long> findGlobalTopSellingIds(Pageable pageable);
}
