package com.datn.pc_store.modules.recommendation.repository;

import com.datn.pc_store.modules.recommendation.entity.AssociationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationRuleRepository extends JpaRepository<AssociationRule, Long> {

    @Query("""
        SELECT ar FROM AssociationRule ar
        JOIN ar.modelRun mr
        WHERE mr.isActive = 1
          AND ar.isActive = 1
          AND (:minLift IS NULL OR ar.lift >= :minLift)
          AND (:minConfidence IS NULL OR ar.confidence >= :minConfidence)
        ORDER BY ar.lift DESC, ar.confidence DESC
    """)
    Page<AssociationRule> findActiveRulesWithFilters(
            @Param("minLift") Double minLift,
            @Param("minConfidence") Double minConfidence,
            Pageable pageable);

    List<AssociationRule> findByModelRunId(Long modelRunId);
}
