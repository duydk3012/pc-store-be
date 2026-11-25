package com.datn.pc_store.modules.recommendation.repository;

import com.datn.pc_store.modules.recommendation.entity.ClusterRecommendation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClusterRecommendationRepository extends JpaRepository<ClusterRecommendation, Long> {

    @Query("""
        SELECT cr FROM ClusterRecommendation cr
        JOIN FETCH cr.recommendedProduct p
        JOIN cr.modelRun mr
        WHERE cr.clusterId = :clusterId
          AND mr.isActive = 1
          AND p.status = com.datn.pc_store.modules.product.enums.ProductStatus.ACTIVE
        ORDER BY cr.rankNo ASC
    """)
    List<ClusterRecommendation> findTopByClusterAndActiveModel(
            @Param("clusterId") Integer clusterId,
            Pageable pageable);
}
