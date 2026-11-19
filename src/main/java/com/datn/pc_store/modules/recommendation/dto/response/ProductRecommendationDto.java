package com.datn.pc_store.modules.recommendation.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendationDto {
    private Long productId;
    private String productCode;
    private String productName;
    private Long price;
    private String imageUrl;
    private String recommendationReason;
    private Double score;
    private boolean inStock;
    private boolean isFallback;
}
