package com.datn.pc_store.modules.recommendation.service.impl;

import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.recommendation.dto.response.PersonalizedRecommendationResponse;
import com.datn.pc_store.modules.recommendation.dto.response.ProductRecommendationDto;
import com.datn.pc_store.modules.recommendation.entity.ClusterRecommendation;
import com.datn.pc_store.modules.recommendation.entity.CustomerCluster;
import com.datn.pc_store.modules.recommendation.entity.ProductRecommendation;
import com.datn.pc_store.modules.recommendation.enums.RecommendationType;
import com.datn.pc_store.modules.recommendation.repository.AssociationRuleItemRepository;
import com.datn.pc_store.modules.recommendation.repository.ClusterRecommendationRepository;
import com.datn.pc_store.modules.recommendation.repository.CustomerClusterRepository;
import com.datn.pc_store.modules.recommendation.repository.ProductRecommendationRepository;
import com.datn.pc_store.modules.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final AssociationRuleItemRepository ruleItemRepo;
    private final ProductRecommendationRepository prodRecRepo;
    private final ClusterRecommendationRepository clusterRecRepo;
    private final CustomerClusterRepository customerClusterRepo;
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;

    @Value("${pcstore.recommendation.min-lift:1.5}")
    private Double defaultMinLift;

    /**
     * BẢO ĐẢM GIỚI HẠN THAM SỐ LIMIT (VALIDATION)
     */
    private int sanitizeLimit(int limit, int min, int max, int defaultVal) {
        if (limit <= 0) return defaultVal;
        return Math.min(limit, max);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRecommendationDto> getFrequentlyBoughtTogether(Long productId, int limit) {
        int finalLimit = sanitizeLimit(limit, 1, 10, 4);
        findActiveProductOrThrow(productId);

        Set<Long> candidateIds = new LinkedHashSet<>();
        Map<Long, Double> liftScoreMap = new HashMap<>();

        // TẦNG 1: Luật kết hợp FP-Growth (Lift >= 1.5, antecedent_count = 1)
        List<Object[]> ruleResults = ruleItemRepo.findConsequentProductsForSingleProduct(
                productId, defaultMinLift, PageRequest.of(0, finalLimit * 3));
        for (Object[] row : ruleResults) {
            Long candId = (Long) row[0];
            Double lift = (Double) row[1];
            if (!candId.equals(productId)) {
                candidateIds.add(candId);
                liftScoreMap.merge(candId, lift, Math::max); // Khử trùng lặp: Lấy Lift lớn nhất
            }
        }

        // TẦNG 2: Fallback sang Phụ kiện tương thích theo cấu hình máy tính (TUYỆT ĐỐI KHÔNG gợi ý Laptop!)
        if (candidateIds.size() < finalLimit) {
            int remain = finalLimit - candidateIds.size();
            List<Long> compAccIds = productRepo.findCompatibleAccessoryIds(productId, PageRequest.of(0, remain * 2));
            for (Long accId : compAccIds) {
                if (!accId.equals(productId) && candidateIds.add(accId)) {
                    liftScoreMap.put(accId, 1.20);
                }
            }
        }

        // TẦNG 3: Fallback sang Top Phụ kiện bán chạy nhất toàn sàn (Best-selling Accessories)
        if (candidateIds.size() < finalLimit) {
            int remain = finalLimit - candidateIds.size();
            List<Long> topAccIds = productRepo.findTopSellingAccessoryIds(PageRequest.of(0, remain * 2));
            for (Long accId : topAccIds) {
                if (!accId.equals(productId) && candidateIds.add(accId)) {
                    liftScoreMap.put(accId, 1.00);
                }
            }
        }

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // BATCH QUERY DUY NHẤT: Kiểm tra tồn kho cho toàn bộ ứng viên (Loại bỏ N+1 Query)
        List<Product> availableProducts = productRepo.findAvailableInStockProductsByIds(candidateIds);
        Map<Long, Product> productMap = availableProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductRecommendationDto> results = new ArrayList<>();
        for (Long id : candidateIds) {
            Product p = productMap.get(id);
            if (p != null) {
                Double score = liftScoreMap.getOrDefault(id, 1.0);
                boolean isFallback = score < defaultMinLift;
                String reason = isFallback ? "Phụ kiện tương thích khuyên dùng" : "Thường mua kèm (Độ nâng Lift: " + String.format("%.2f", score) + ")";
                results.add(mapToDto(p, reason, score, true, isFallback));
                if (results.size() >= finalLimit) break;
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRecommendationDto> getSimilarProducts(Long productId, int limit) {
        int finalLimit = sanitizeLimit(limit, 1, 10, 4);
        Product currentProduct = findActiveProductOrThrow(productId);

        Set<Long> candidateIds = new LinkedHashSet<>();
        Map<Long, Double> simScoreMap = new HashMap<>();

        // TẦNG 1: Content-Based Cosine Similarity đã tính toán trước
        List<ProductRecommendation> precomputed = prodRecRepo.findActiveSimilarBySource(
                productId, RecommendationType.SIMILAR_CONTENT, PageRequest.of(0, finalLimit * 2));
        for (ProductRecommendation pr : precomputed) {
            Long recId = pr.getRecommendedProduct().getId();
            if (!recId.equals(productId)) {
                candidateIds.add(recId);
                simScoreMap.put(recId, pr.getScore());
            }
        }

        // TẦNG 2: Fallback sang sản phẩm CÙNG DANH MỤC trong khoảng giá +-20% (Tái sử dụng currentProduct)
        if (candidateIds.size() < finalLimit && currentProduct.getPrice() != null) {
            long minPrice = (long) (currentProduct.getPrice() * 0.8);
            long maxPrice = (long) (currentProduct.getPrice() * 1.2);
            List<Long> catIds = productRepo.findIdsByCategoryAndPriceRange(
                    currentProduct.getId(), minPrice, maxPrice, PageRequest.of(0, (finalLimit - candidateIds.size()) * 2));
            for (Long id : catIds) {
                if (candidateIds.add(id)) simScoreMap.put(id, 0.70);
            }
        }

        // TẦNG 3: Fallback sang Top Laptop bán chạy nhất cùng thương hiệu và cùng loại (product_type = 1)
        if (candidateIds.size() < finalLimit && currentProduct.getBrandId() != null) {
            List<Long> brandIds = productRepo.findTopSellingLaptopIdsByBrand(
                    currentProduct.getBrandId(), currentProduct.getId(), PageRequest.of(0, finalLimit - candidateIds.size()));
            for (Long id : brandIds) {
                if (candidateIds.add(id)) simScoreMap.put(id, 0.50);
            }
        }

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // BATCH QUERY TỒN KHO DUY NHẤT
        List<Product> availableProducts = productRepo.findAvailableInStockProductsByIds(candidateIds);
        Map<Long, Product> productMap = availableProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductRecommendationDto> results = new ArrayList<>();
        for (Long id : candidateIds) {
            Product p = productMap.get(id);
            if (p != null) {
                Double score = simScoreMap.getOrDefault(id, 0.5);
                boolean isFallback = score < 0.75;
                String reason = isFallback ? "Cùng phân khúc cấu hình & thương hiệu" : "Cấu hình tương đồng phần cứng";
                results.add(mapToDto(p, reason, score, true, isFallback));
                if (results.size() >= finalLimit) break;
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalizedRecommendationResponse getPersonalizedRecommendations(Long customerId, int limit) {
        int finalLimit = sanitizeLimit(limit, 1, 20, 6);
        PersonalizedRecommendationResponse response = new PersonalizedRecommendationResponse();
        Set<Long> candidateIds = new LinkedHashSet<>();
        Map<Long, Double> scoreMap = new HashMap<>();

        // Kiểm tra xem khách hàng có tồn tại và đã được phân cụm chưa
        Optional<CustomerCluster> clusterOpt = (customerId != null)
                ? customerClusterRepo.findActiveByCustomerId(customerId)
                : Optional.empty();

        // TẦNG 1: Khách hàng đã thuộc cụm K-Means
        if (clusterOpt.isPresent()) {
            CustomerCluster cc = clusterOpt.get();
            response.setCustomerId(customerId);
            response.setClusterId(cc.getClusterId());
            response.setClusterLabel(cc.getClusterLabel());
            response.setStrategy("KMEANS_CLUSTER_PERSONA");

            List<ClusterRecommendation> clusterRecs = clusterRecRepo.findTopByClusterAndActiveModel(
                    cc.getClusterId(), PageRequest.of(0, finalLimit * 2));
            for (ClusterRecommendation cr : clusterRecs) {
                candidateIds.add(cr.getRecommendedProduct().getId());
                scoreMap.put(cr.getRecommendedProduct().getId(), cr.getScore());
            }
        }

        // TẦNG 2: Nếu chưa có cụm HOẶC Tầng 1 rỗng/hết hàng -> Gợi ý theo sản phẩm đã mua / tương tác gần nhất
        if (candidateIds.size() < finalLimit && customerId != null) {
            List<Long> recentPurchasedIds = orderItemRepo.findRecentPurchasedProductIds(customerId, PageRequest.of(0, 3));
            for (Long recentId : recentPurchasedIds) {
                List<ProductRecommendation> simList = prodRecRepo.findActiveSimilarBySource(
                        recentId, RecommendationType.SIMILAR_CONTENT, PageRequest.of(0, 3));
                for (ProductRecommendation pr : simList) {
                    if (candidateIds.add(pr.getRecommendedProduct().getId())) {
                        scoreMap.put(pr.getRecommendedProduct().getId(), pr.getScore() * 0.9);
                    }
                }
                if (candidateIds.size() >= finalLimit) break;
            }
            if (response.getStrategy() == null && !candidateIds.isEmpty()) {
                response.setCustomerId(customerId);
                response.setStrategy("RECENT_INTERACTION_CONTENT_BASED");
                response.setIsFallback(true);
                response.setFallbackReason("Chưa đủ dữ liệu phân cụm, đề xuất dựa trên sản phẩm tương tác gần nhất.");
            }
        }

        // TẦNG 3: Khách vãng lai hoặc User mới hoàn toàn (Cold-Start) -> Top Best-Sellers toàn hệ thống
        if (candidateIds.size() < finalLimit) {
            List<Long> topSellerIds = productRepo.findGlobalTopSellingIds(PageRequest.of(0, finalLimit * 2));
            for (Long id : topSellerIds) {
                if (candidateIds.add(id)) scoreMap.put(id, 1.00);
            }
            if (response.getStrategy() == null) {
                response.setCustomerId(customerId);
                response.setClusterLabel("Khách vãng lai / Người dùng mới");
                response.setStrategy("GLOBAL_POPULARITY_BEST_SELLER");
                response.setIsFallback(true);
                response.setFallbackReason("Chưa có lịch sử giao dịch. Đề xuất sản phẩm thịnh hành nhất hệ thống.");
            }
        }

        if (candidateIds.isEmpty()) {
            response.setProducts(List.of());
            if (response.getIsFallback() == null) response.setIsFallback(false);
            return response;
        }

        // BATCH QUERY TỒN KHO DUY NHẤT
        List<Product> availableProducts = productRepo.findAvailableInStockProductsByIds(candidateIds);
        Map<Long, Product> productMap = availableProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductRecommendationDto> productDtos = new ArrayList<>();
        for (Long id : candidateIds) {
            Product p = productMap.get(id);
            if (p != null) {
                Double score = scoreMap.getOrDefault(id, 1.0);
                String reason = Boolean.TRUE.equals(response.getIsFallback())
                        ? "Sản phẩm nổi bật được quan tâm nhiều"
                        : "Phù hợp với nhóm nhu cầu " + response.getClusterLabel() + " của bạn";
                productDtos.add(mapToDto(p, reason, score, true, Boolean.TRUE.equals(response.getIsFallback())));
                if (productDtos.size() >= finalLimit) break;
            }
        }

        response.setProducts(productDtos);
        if (response.getIsFallback() == null) response.setIsFallback(false);
        return response;
    }

    private Product findActiveProductOrThrow(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));
        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new ResourceNotFoundException("Sản phẩm", "id", productId);
        }
        return product;
    }

    private ProductRecommendationDto mapToDto(Product p, String reason, Double score, boolean inStock, boolean isFallback) {
        return ProductRecommendationDto.builder()
                .productId(p.getId())
                .productCode(p.getCode())
                .productName(p.getName())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .recommendationReason(reason)
                .score(score)
                .inStock(inStock)
                .isFallback(isFallback)
                .build();
    }
}
