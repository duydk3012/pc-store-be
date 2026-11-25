package com.datn.pc_store.modules.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "association_rules")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssociationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", length = 64)
    private String ruleCode;

    @Column(name = "antecedent_count", nullable = false)
    @Builder.Default
    private Integer antecedentCount = 1;

    @Column(name = "consequent_count", nullable = false)
    @Builder.Default
    private Integer consequentCount = 1;

    @Column(name = "antecedents_summary", columnDefinition = "TEXT")
    private String antecedentsSummary;

    @Column(name = "consequents_summary", columnDefinition = "TEXT")
    private String consequentsSummary;

    @Column(nullable = false)
    private Double support;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private Double lift;

    @Column
    private Double leverage;

    @Column
    private Double conviction;

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private Integer transactionCount = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_run_id", nullable = false)
    private MiningModelRun modelRun;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Integer isActive = 1;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    private Instant createdDate;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssociationRuleItem> items = new ArrayList<>();
}
