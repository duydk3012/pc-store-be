package com.datn.pc_store.modules.recommendation.entity;

import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.recommendation.enums.RecommendationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "product_recommendations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_source_recommended_type_run", columnNames = {
        "source_product_id", "recommended_product_id", "recommendation_type", "model_run_id"
    })
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 50)
    private RecommendationType recommendationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_product_id", nullable = false)
    private Product sourceProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommended_product_id", nullable = false)
    private Product recommendedProduct;

    @Column(nullable = false)
    @Builder.Default
    private Double score = 0.0;

    @Column(name = "rank_no", nullable = false)
    @Builder.Default
    private Integer rankNo = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_run_id", nullable = false)
    private MiningModelRun modelRun;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    private Instant createdDate;
}
