package com.datn.pc_store.modules.product.repository;

import com.datn.pc_store.modules.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByProductIdIn(Collection<Long> productIds);

    @Modifying
    @Query("DELETE FROM ProductCategory pc WHERE pc.productId = :productId")
    void deleteLinksByProductId(@Param("productId") Long productId);
}
