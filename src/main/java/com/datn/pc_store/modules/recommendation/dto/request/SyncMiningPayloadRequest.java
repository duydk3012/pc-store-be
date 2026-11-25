package com.datn.pc_store.modules.recommendation.dto.request;

import com.datn.pc_store.modules.recommendation.enums.DataOrigin;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncMiningPayloadRequest {

    @NotNull(message = "modelType không được để trống")
    private ModelType modelType;

    @NotBlank(message = "algorithmName không được để trống")
    private String algorithmName;

    private DataOrigin dataOrigin;
    private String datasetVersion;

    @Min(value = 0, message = "datasetRowCount không được âm")
    private Integer datasetRowCount;

    private Map<String, Object> parameters;
    private Map<String, Object> metrics;
    private String notes;

    // Association Rules payload
    @Valid
    private List<RulePayload> rules;

    // Customer Clustering payload
    @Valid
    private List<ClusterPayload> clusters;
    @Valid
    private List<ClusterRecPayload> clusterRecommendations;
    @Valid
    private List<CustomerAssignmentPayload> customerAssignments;

    // Content-Based payload
    @Valid
    private List<ContentRecPayload> contentRecommendations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RulePayload {
        private String ruleCode;
        @Positive(message = "antecedentCount phải lớn hơn 0")
        private Integer antecedentCount;
        @Positive(message = "consequentCount phải lớn hơn 0")
        private Integer consequentCount;
        @NotEmpty(message = "antecedentProductIds không được rỗng")
        private List<@NotNull @Positive Long> antecedentProductIds;
        @NotEmpty(message = "consequentProductIds không được rỗng")
        private List<@NotNull @Positive Long> consequentProductIds;
        private String antecedentsSummary;
        private String consequentsSummary;
        @NotNull(message = "support không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "support phải lớn hơn 0")
        private Double support;
        @NotNull(message = "confidence không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "confidence phải lớn hơn 0")
        private Double confidence;
        @NotNull(message = "lift không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "lift phải lớn hơn 0")
        private Double lift;
        private Double leverage;
        private Double conviction;
        @Min(value = 0, message = "transactionCount không được âm")
        private Integer transactionCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClusterPayload {
        @NotNull(message = "clusterId không được để trống")
        private Integer clusterId;
        private String clusterLabel;
        @Min(value = 0, message = "customerCount không được âm")
        private Integer customerCount;
        @Min(value = 0, message = "percentage không được âm")
        private Double percentage;
        private List<Double> centroid;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClusterRecPayload {
        @NotNull(message = "clusterId không được để trống")
        private Integer clusterId;
        @NotNull(message = "recommendedProductId không được để trống")
        @Positive(message = "recommendedProductId phải lớn hơn 0")
        private Long recommendedProductId;
        @NotNull(message = "score không được để trống")
        private Double score;
        @Positive(message = "rankNo phải lớn hơn 0")
        private Integer rankNo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerAssignmentPayload {
        @NotNull(message = "customerId không được để trống")
        @Positive(message = "customerId phải lớn hơn 0")
        private Long customerId;
        @NotNull(message = "clusterId không được để trống")
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
        @NotNull(message = "sourceProductId không được để trống")
        @Positive(message = "sourceProductId phải lớn hơn 0")
        private Long sourceProductId;
        @NotNull(message = "recommendedProductId không được để trống")
        @Positive(message = "recommendedProductId phải lớn hơn 0")
        private Long recommendedProductId;
        @NotNull(message = "score không được để trống")
        private Double score;
        @Positive(message = "rankNo phải lớn hơn 0")
        private Integer rankNo;
    }
}
