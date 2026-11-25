package com.datn.pc_store.modules.recommendation.repository;

import com.datn.pc_store.modules.recommendation.entity.ProductRecommendation;
import com.datn.pc_store.modules.recommendation.enums.RecommendationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {

    @Query("""
        SELECT pr FROM ProductRecommendation pr
        JOIN FETCH pr.recommendedProduct rp
        JOIN pr.modelRun mr
        WHERE pr.sourceProduct.id = :sourceProductId
          AND pr.recommendationType = :type
          AND mr.isActive = 1
          AND rp.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
        ORDER BY pr.rankNo ASC
    """)
    List<ProductRecommendation> findActiveSimilarBySource(
            @Param("sourceProductId") Long sourceProductId,
            @Param("type") RecommendationType type,
            Pageable pageable);
}
