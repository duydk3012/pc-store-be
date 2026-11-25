package com.datn.pc_store.modules.recommendation.repository;

import com.datn.pc_store.modules.recommendation.entity.AssociationRuleItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationRuleItemRepository extends JpaRepository<AssociationRuleItem, Long> {

    /**
     * Tìm các sản phẩm CONSEQUENT được mua kèm cho trường hợp xem 1 sản phẩm nguồn (Trang Chi tiết).
     * Yêu cầu:
     * - Chỉ chọn luật có antecedentCount = 1.
     * - Tham số minLift được lọc động (mặc định >= 1.5).
     * - Sắp xếp theo Lift giảm dần, Confidence giảm dần.
     */
    @Query("""
        SELECT consItem.product.id, ar.lift, ar.confidence, ar.support
        FROM AssociationRuleItem antItem
        JOIN antItem.rule ar
        JOIN ar.modelRun mr
        JOIN AssociationRuleItem consItem ON ar.id = consItem.rule.id
        WHERE antItem.product.id = :productId
          AND antItem.itemType = com.datn.pc_store.modules.recommendation.enums.RuleItemType.ANTECEDENT
          AND consItem.itemType = com.datn.pc_store.modules.recommendation.enums.RuleItemType.CONSEQUENT
          AND ar.antecedentCount = 1
          AND ar.lift >= :minLift
          AND ar.isActive = 1
          AND mr.isActive = 1
        ORDER BY ar.lift DESC, ar.confidence DESC
    """)
    List<Object[]> findConsequentProductsForSingleProduct(
            @Param("productId") Long productId,
            @Param("minLift") Double minLift,
            Pageable pageable);
}
