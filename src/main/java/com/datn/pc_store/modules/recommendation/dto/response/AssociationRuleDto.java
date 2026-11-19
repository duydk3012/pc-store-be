package com.datn.pc_store.modules.recommendation.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssociationRuleDto {
    private Long id;
    private String ruleCode;
    private Integer antecedentCount;
    private Integer consequentCount;
    private List<Long> antecedentProductIds;
    private List<Long> consequentProductIds;
    private String antecedentsSummary;
    private String consequentsSummary;
    private Double support;
    private Double confidence;
    private Double lift;
    private Integer transactionCount;
    private Integer isActive;
}
