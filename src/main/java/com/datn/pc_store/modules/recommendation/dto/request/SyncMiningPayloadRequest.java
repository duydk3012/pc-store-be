package com.datn.pc_store.modules.recommendation.dto.request;

import com.datn.pc_store.modules.recommendation.enums.DataOrigin;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncMiningPayloadRequest {

    private ModelType modelType;
    private String algorithmName;
    private DataOrigin dataOrigin;
    private String datasetVersion;
    private Integer datasetRowCount;
    private Map<String, Object> parameters;
    private Map<String, Object> metrics;
    private String notes;

    // Association Rules payload
    private List<RulePayload> rules;

    // Customer Clustering payload
    private List<ClusterPayload> clusters;
    private List<ClusterRecPayload> clusterRecommendations;
    private List<CustomerAssignmentPayload> customerAssignments;

    // Content-Based payload
    private List<ContentRecPayload> contentRecommendations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RulePayload {
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
        private Double leverage;
        private Double conviction;
        private Integer transactionCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClusterPayload {
        private Integer clusterId;
        private String clusterLabel;
        private Integer customerCount;
        private Double percentage;
        private List<Double> centroid;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClusterRecPayload {
        private Integer clusterId;
        private Long recommendedProductId;
        private Double score;
        private Integer rankNo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerAssignmentPayload {
        private Long customerId;
        private Integer clusterId;
        private String clusterLabel;
        private Double distanceToCentroid;
        private Map<String, Object> featuresJson;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContentRecPayload {
        private String recommendationType;
        private Long sourceProductId;
        private Long recommendedProductId;
        private Double score;
        private Integer rankNo;
    }
}
