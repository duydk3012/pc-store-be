package com.datn.pc_store.modules.recommendation.controller;

import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.recommendation.dto.response.PersonalizedRecommendationResponse;
import com.datn.pc_store.modules.recommendation.dto.response.ProductRecommendationDto;
import com.datn.pc_store.modules.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recommendation System", description = "Gợi ý sản phẩm thông minh: Thường mua cùng (FP-Growth), Tương tự (Content-based), và Cá nhân hóa (K-Means)")
public class RecommendationPublicController {

    private final RecommendationService recommendationService;

    @GetMapping("/products/{id}/frequently-bought-together")
    @Operation(summary = "Gợi ý sản phẩm thường mua cùng nhau (Frequently Bought Together)",
            description = "Dựa trên mô hình Khai phá luật kết hợp (FP-Growth) và lọc tồn kho thực tế; tự động fallback theo danh mục phụ kiện và top-seller khi thiếu dữ liệu.")
    public ResponseEntity<List<ProductRecommendationDto>> getFrequentlyBoughtTogether(
            @PathVariable("id") Long productId,
            @RequestParam(defaultValue = "4") @Min(1) @Max(10) int limit) {
        return ResponseEntity.ok(recommendationService.getFrequentlyBoughtTogether(productId, limit));
    }

    @GetMapping("/products/{id}/similar")
    @Operation(summary = "Gợi ý sản phẩm tương đương / thay thế (Similar Products)",
            description = "Dựa trên độ tương đồng đặc trưng sản phẩm (Content-Based) cùng phân khúc giá; tự động fallback theo cùng phân khúc giá và thương hiệu khi thiếu dữ liệu.")
    public ResponseEntity<List<ProductRecommendationDto>> getSimilarProducts(
            @PathVariable("id") Long productId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(10) int limit) {
        return ResponseEntity.ok(recommendationService.getSimilarProducts(productId, limit));
    }

    @GetMapping("/personalized")
    @Operation(summary = "Gợi ý sản phẩm cá nhân hóa cho người dùng (Personalized Recommendations)",
            description = "Dựa trên phân cụm hành vi khách hàng K-Means (hoặc lịch sử mua sắm gần đây); tự động fallback sang Top-seller toàn hệ thống cho khách vãng lai hoặc khách hàng mới.")
    public ResponseEntity<PersonalizedRecommendationResponse> getPersonalizedRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        Long customerId = (userDetails != null) ? userDetails.getId() : null;
        return ResponseEntity.ok(recommendationService.getPersonalizedRecommendations(customerId, limit));
    }
}
