package com.datn.pc_store.modules.recommendation.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedRecommendationResponse {
    private Long customerId;
    private Integer clusterId;
    private String clusterLabel;
    private String strategy;
    private Boolean isFallback;
    private String fallbackReason;

    @Builder.Default
    private List<ProductRecommendationDto> products = new ArrayList<>();
}
