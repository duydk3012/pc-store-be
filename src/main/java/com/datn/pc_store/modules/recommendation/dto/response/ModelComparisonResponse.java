package com.datn.pc_store.modules.recommendation.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelComparisonResponse {

    private ModelVersionSummaryDto sourceModel;
    private ModelVersionSummaryDto targetModel;

    // So sánh các chỉ số độ đo (Silhouette, Davies-Bouldin, Calinski-Harabasz, Inertia...)
    private Map<String, MetricDiff> metricsDiff;

    // So sánh sự chuyển dịch phân cụm (Cluster Drift)
    private List<ClusterDriftDto> clusterDrifts;

    // So sánh sự chuyển dịch luật kết hợp (Association Rules Drift)
    private RuleDriftSummaryDto ruleDriftSummary;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricDiff {
        private Double sourceValue;
        private Double targetValue;
        private Double changeAmount;
        private Double changePercentage;
        private String evaluation; // BETTER, WORSE, STABLE
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClusterDriftDto {
        private Integer clusterId;
        private String sourceLabel;
        private String targetLabel;
        private Integer sourceCount;
        private Integer targetCount;
        private Double sourcePercentage;
        private Double targetPercentage;
        private Double percentageDiff;
        private String driftAnalysis;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RuleDriftSummaryDto {
        private Integer sourceTotalRules;
        private Integer targetTotalRules;
        private Integer commonRulesCount;
        private Integer emergingRulesCount;
        private Integer droppedRulesCount;
        private Double sourceAvgLift;
        private Double targetAvgLift;
        private List<AssociationRuleDto> topEmergingRules;
    }
}
