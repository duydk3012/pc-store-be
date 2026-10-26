package com.datn.pc_store.modules.productprice.repository;

import com.datn.pc_store.modules.productprice.entity.ProductPrice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProductPrice> findByProductIdAndPresentPriceTrue(Long productId);

    @Query("""
        SELECT p FROM ProductPrice p WHERE p.productId IN :productIds AND p.presentPrice=true
          AND (p.effectiveFrom IS NULL OR p.effectiveFrom<=:at)
          AND (p.effectiveTo IS NULL OR p.effectiveTo>:at)
        """)
    List<ProductPrice> findCurrentPrices(@Param("productIds") Collection<Long> productIds, @Param("at") Instant at);

    @Query("""
        SELECT p FROM ProductPrice p WHERE (:productId IS NULL OR p.productId=:productId)
          AND (:presentPrice IS NULL OR p.presentPrice=:presentPrice)
        """)
    Page<ProductPrice> searchPrices(@Param("productId") Long productId,
                                   @Param("presentPrice") Boolean presentPrice, Pageable pageable);
}
