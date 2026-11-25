package com.datn.pc_store.modules.recommendation.service;

import com.datn.pc_store.modules.recommendation.dto.response.PersonalizedRecommendationResponse;
import com.datn.pc_store.modules.recommendation.dto.response.ProductRecommendationDto;

import java.util.List;

public interface RecommendationService {

    /**
     * Gợi ý Phụ kiện / Combo Thường mua cùng (Frequently Bought Together - Cross-sell)
     * Fallback 3 tầng:
     * - Tầng 1: Luật kết hợp FP-Growth (Lift >= 1.5, antecedent_count = 1).
     * - Tầng 2: Phụ kiện tương thích theo danh mục máy tính.
     * - Tầng 3: Top phụ kiện bán chạy nhất toàn sàn.
     */
    List<ProductRecommendationDto> getFrequentlyBoughtTogether(Long productId, int limit);

    /**
     * Gợi ý Cấu hình tương đồng (Similar Content)
     * Fallback 3 tầng:
     * - Tầng 1: Content-Based Cosine Similarity tính toán trước.
     * - Tầng 2: Sản phẩm cùng danh mục trong khoảng giá +-20%.
     * - Tầng 3: Top Laptop bán chạy nhất cùng thương hiệu.
     */
    List<ProductRecommendationDto> getSimilarProducts(Long productId, int limit);

    /**
     * Gợi ý Cá nhân hóa cho người dùng (Personalized Feed)
     * Fallback 3 tầng:
     * - Tầng 1: Phân cụm K-Means Persona tra từ cluster_recommendations.
     * - Tầng 2: Gợi ý theo sản phẩm đã mua / tương tác gần nhất của khách hàng.
     * - Tầng 3: Top Best-Sellers toàn hệ thống cho khách vãng lai.
     */
    PersonalizedRecommendationResponse getPersonalizedRecommendations(Long customerId, int limit);
}
