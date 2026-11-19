package com.datn.pc_store.modules.recommendation.service.impl;

import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.recommendation.dto.request.SyncMiningPayloadRequest;
import com.datn.pc_store.modules.recommendation.dto.response.*;
import com.datn.pc_store.modules.recommendation.entity.*;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import com.datn.pc_store.modules.recommendation.enums.RecommendationType;
import com.datn.pc_store.modules.recommendation.enums.RuleItemType;
import com.datn.pc_store.modules.recommendation.enums.RunStatus;
import com.datn.pc_store.modules.recommendation.repository.*;
import com.datn.pc_store.modules.recommendation.service.DataMiningSyncService;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataMiningSyncServiceImpl implements DataMiningSyncService {

    private final MiningModelRunRepository modelRunRepository;
    private final AssociationRuleRepository associationRuleRepository;
    private final CustomerClusterRepository customerClusterRepository;
    private final ClusterRecommendationRepository clusterRecommendationRepository;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    @Transactional
    public MiningModelRunDto syncModelData(SyncMiningPayloadRequest request) {
        log.info("Starting sync for model type: {}, algorithm: {}", request.getModelType(), request.getAlgorithmName());

        String parametersJson = toJsonString(request.getParameters());
        String metricsJson = toJsonString(request.getMetrics());

        MiningModelRun modelRun = MiningModelRun.builder()
                .modelType(request.getModelType())
                .algorithmName(request.getAlgorithmName())
                .runStatus(RunStatus.RUNNING)
                .isActive(0)
                .dataOrigin(request.getDataOrigin() != null ? request.getDataOrigin() : com.datn.pc_store.modules.recommendation.enums.DataOrigin.REAL)
                .datasetVersion(request.getDatasetVersion())
                .datasetRowCount(request.getDatasetRowCount() != null ? request.getDatasetRowCount() : 0)
                .parametersJson(parametersJson)
                .metricsJson(metricsJson)
                .notes(request.getNotes())
                .startedAt(Instant.now())
                .build();

        modelRun = modelRunRepository.save(modelRun);

        try {
            if (request.getModelType() == ModelType.ASSOCIATION_RULES) {
                syncAssociationRules(modelRun, request.getRules());
            } else if (request.getModelType() == ModelType.CUSTOMER_CLUSTERING) {
                syncCustomerClustering(modelRun, request.getClusterRecommendations(), request.getCustomerAssignments());
            } else if (request.getModelType() == ModelType.CONTENT_BASED) {
                syncContentBased(modelRun, request.getContentRecommendations());
            }

            // Deactivate older active models of the same type
            modelRunRepository.deactivateExistingModels(request.getModelType());

            // Mark this run as SUCCESS and ACTIVE
            modelRun.setRunStatus(RunStatus.SUCCESS);
            modelRun.setIsActive(1);
            modelRun.setFinishedAt(Instant.now());
            modelRun = modelRunRepository.save(modelRun);

            log.info("Model sync completed successfully. RunId: {}, Type: {}", modelRun.getId(), modelRun.getModelType());
            return mapToModelRunDto(modelRun);

        } catch (Exception e) {
            log.error("Model sync failed for RunId: {}", modelRun.getId(), e);
            modelRun.setRunStatus(RunStatus.FAILED);
            modelRun.setFinishedAt(Instant.now());
            modelRun.setNotes("Sync failed: " + e.getMessage());
            modelRunRepository.save(modelRun);
            throw new RuntimeException("Lỗi đồng bộ mô hình khai phá dữ liệu: " + e.getMessage(), e);
        }
    }

    private void syncAssociationRules(MiningModelRun modelRun, List<SyncMiningPayloadRequest.RulePayload> rulePayloads) {
        if (rulePayloads == null || rulePayloads.isEmpty()) {
            log.warn("No association rules found in payload for RunId: {}", modelRun.getId());
            return;
        }

        Set<Long> productIds = new HashSet<>();
        for (SyncMiningPayloadRequest.RulePayload rp : rulePayloads) {
            if (rp.getAntecedentProductIds() != null) {
                productIds.addAll(rp.getAntecedentProductIds());
            }
            if (rp.getConsequentProductIds() != null) {
                productIds.addAll(rp.getConsequentProductIds());
            }
        }

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<AssociationRule> rulesToSave = new ArrayList<>();

        for (SyncMiningPayloadRequest.RulePayload rp : rulePayloads) {
            AssociationRule rule = AssociationRule.builder()
                    .ruleCode(rp.getRuleCode())
                    .antecedentCount(rp.getAntecedentCount() != null ? rp.getAntecedentCount() : 1)
                    .consequentCount(rp.getConsequentCount() != null ? rp.getConsequentCount() : 1)
                    .antecedentsSummary(rp.getAntecedentsSummary())
                    .consequentsSummary(rp.getConsequentsSummary())
                    .support(rp.getSupport() != null ? rp.getSupport() : 0.0)
                    .confidence(rp.getConfidence() != null ? rp.getConfidence() : 0.0)
                    .lift(rp.getLift() != null ? rp.getLift() : 0.0)
                    .leverage(rp.getLeverage())
                    .conviction(rp.getConviction())
                    .transactionCount(rp.getTransactionCount() != null ? rp.getTransactionCount() : 0)
                    .modelRun(modelRun)
                    .isActive(1)
                    .items(new ArrayList<>())
                    .build();

            if (rp.getAntecedentProductIds() != null) {
                for (Long pId : rp.getAntecedentProductIds()) {
                    Product p = productMap.get(pId);
                    if (p != null) {
                        rule.getItems().add(AssociationRuleItem.builder()
                                .rule(rule)
                                .product(p)
                                .itemType(RuleItemType.ANTECEDENT)
                                .build());
                    }
                }
            }

            if (rp.getConsequentProductIds() != null) {
                for (Long pId : rp.getConsequentProductIds()) {
                    Product p = productMap.get(pId);
                    if (p != null) {
                        rule.getItems().add(AssociationRuleItem.builder()
                                .rule(rule)
                                .product(p)
                                .itemType(RuleItemType.CONSEQUENT)
                                .build());
                    }
                }
            }

            rulesToSave.add(rule);
        }

        associationRuleRepository.saveAll(rulesToSave);
        log.info("Saved {} association rules with their items.", rulesToSave.size());
    }

    private void syncCustomerClustering(MiningModelRun modelRun,
                                        List<SyncMiningPayloadRequest.ClusterRecPayload> clusterRecs,
                                        List<SyncMiningPayloadRequest.CustomerAssignmentPayload> customerAssignments) {
        if (clusterRecs != null && !clusterRecs.isEmpty()) {
            Set<Long> productIds = clusterRecs.stream()
                    .map(SyncMiningPayloadRequest.ClusterRecPayload::getRecommendedProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            List<ClusterRecommendation> recsToSave = new ArrayList<>();
            for (SyncMiningPayloadRequest.ClusterRecPayload cr : clusterRecs) {
                Product p = productMap.get(cr.getRecommendedProductId());
                if (p != null) {
                    recsToSave.add(ClusterRecommendation.builder()
                            .clusterId(cr.getClusterId())
                            .recommendedProduct(p)
                            .score(cr.getScore() != null ? cr.getScore() : 0.0)
                            .rankNo(cr.getRankNo() != null ? cr.getRankNo() : 1)
                            .modelRun(modelRun)
                            .build());
                }
            }
            clusterRecommendationRepository.saveAll(recsToSave);
            log.info("Saved {} cluster recommendations.", recsToSave.size());
        }

        if (customerAssignments != null && !customerAssignments.isEmpty()) {
            Set<Long> customerIds = customerAssignments.stream()
                    .map(SyncMiningPayloadRequest.CustomerAssignmentPayload::getCustomerId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, User> userMap = userRepository.findAllById(customerIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            List<CustomerCluster> assignmentsToSave = new ArrayList<>();
            for (SyncMiningPayloadRequest.CustomerAssignmentPayload ca : customerAssignments) {
                User user = userMap.get(ca.getCustomerId());
                if (user != null) {
                    assignmentsToSave.add(CustomerCluster.builder()
                            .customer(user)
                            .clusterId(ca.getClusterId())
                            .clusterLabel(ca.getClusterLabel() != null ? ca.getClusterLabel() : "Cluster " + ca.getClusterId())
                            .distanceToCentroid(ca.getDistanceToCentroid())
                            .featuresJson(toJsonString(ca.getFeaturesJson()))
                            .modelRun(modelRun)
                            .assignedAt(Instant.now())
                            .build());
                }
            }
            customerClusterRepository.saveAll(assignmentsToSave);
            log.info("Saved {} customer cluster assignments.", assignmentsToSave.size());
        }
    }

    private void syncContentBased(MiningModelRun modelRun, List<SyncMiningPayloadRequest.ContentRecPayload> contentRecs) {
        if (contentRecs == null || contentRecs.isEmpty()) {
            log.warn("No content-based recommendations in payload for RunId: {}", modelRun.getId());
            return;
        }

        Set<Long> productIds = new HashSet<>();
        for (SyncMiningPayloadRequest.ContentRecPayload cr : contentRecs) {
            if (cr.getSourceProductId() != null) productIds.add(cr.getSourceProductId());
            if (cr.getRecommendedProductId() != null) productIds.add(cr.getRecommendedProductId());
        }

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ProductRecommendation> recsToSave = new ArrayList<>();
        for (SyncMiningPayloadRequest.ContentRecPayload cr : contentRecs) {
            Product src = productMap.get(cr.getSourceProductId());
            Product rec = productMap.get(cr.getRecommendedProductId());
            if (src != null && rec != null) {
                RecommendationType type = RecommendationType.SIMILAR_CONTENT;
                if (cr.getRecommendationType() != null) {
                    try {
                        type = RecommendationType.valueOf(cr.getRecommendationType().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                recsToSave.add(ProductRecommendation.builder()
                        .recommendationType(type)
                        .sourceProduct(src)
                        .recommendedProduct(rec)
                        .score(cr.getScore() != null ? cr.getScore() : 0.0)
                        .rankNo(cr.getRankNo() != null ? cr.getRankNo() : 1)
                        .modelRun(modelRun)
                        .build());
            }
        }

        productRecommendationRepository.saveAll(recsToSave);
        log.info("Saved {} content-based product recommendations.", recsToSave.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiningModelRunDto> getAllModelRuns() {
        return modelRunRepository.findAllByOrderByCreatedDateDesc().stream()
                .map(this::mapToModelRunDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MiningModelRunDto getLatestActiveModel(ModelType modelType) {
        return modelRunRepository.findFirstByModelTypeAndIsActiveOrderByCreatedDateDesc(modelType, 1)
                .map(this::mapToModelRunDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerClusterDto> getClusterSummaries() {
        List<Object[]> rawStats = customerClusterRepository.getClusterStatsFromActiveModel();
        if (rawStats.isEmpty()) {
            return Collections.emptyList();
        }

        long totalCustomers = 0;
        for (Object[] row : rawStats) {
            Long count = (Long) row[2];
            if (count != null) {
                totalCustomers += count;
            }
        }

        List<CustomerClusterDto> dtos = new ArrayList<>();
        for (Object[] row : rawStats) {
            Integer clusterId = (Integer) row[0];
            String clusterLabel = (String) row[1];
            Long count = (Long) row[2];
            double percentage = totalCustomers > 0 ? (count * 100.0 / totalCustomers) : 0.0;

            dtos.add(CustomerClusterDto.builder()
                    .clusterId(clusterId)
                    .clusterLabel(clusterLabel)
                    .customerCount(count != null ? count.intValue() : 0)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .centroid(Collections.emptyList())
                    .build());
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssociationRuleDto> getActiveAssociationRules(Double minLift, Double minConfidence, Pageable pageable) {
        Page<AssociationRule> rulesPage = associationRuleRepository.findActiveRulesWithFilters(minLift, minConfidence, pageable);
        return rulesPage.map(rule -> {
            List<Long> antecedents = new ArrayList<>();
            List<Long> consequents = new ArrayList<>();
            if (rule.getItems() != null) {
                for (AssociationRuleItem item : rule.getItems()) {
                    if (item.getItemType() == RuleItemType.ANTECEDENT) {
                        antecedents.add(item.getProduct().getId());
                    } else if (item.getItemType() == RuleItemType.CONSEQUENT) {
                        consequents.add(item.getProduct().getId());
                    }
                }
            }

            return AssociationRuleDto.builder()
                    .id(rule.getId())
                    .ruleCode(rule.getRuleCode())
                    .antecedentCount(rule.getAntecedentCount())
                    .consequentCount(rule.getConsequentCount())
                    .antecedentProductIds(antecedents)
                    .consequentProductIds(consequents)
                    .antecedentsSummary(rule.getAntecedentsSummary())
                    .consequentsSummary(rule.getConsequentsSummary())
                    .support(rule.getSupport())
                    .confidence(rule.getConfidence())
                    .lift(rule.getLift())
                    .transactionCount(rule.getTransactionCount())
                    .isActive(rule.getIsActive())
                    .build();
        });
    }

    // =========================================================================
    // BƯỚC 2: MODEL REGISTRY, VERSION MANAGEMENT & ROLLBACK SWITCHER
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ModelVersionSummaryDto> getModelHistory(ModelType modelType, Pageable pageable) {
        Page<MiningModelRun> page = (modelType != null)
                ? modelRunRepository.findByModelTypeOrderByCreatedDateDesc(modelType, pageable)
                : modelRunRepository.findAllByOrderByCreatedDateDesc(pageable);
        return page.map(this::mapToSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVersionSummaryDto getModelDetails(Long modelRunId) {
        MiningModelRun run = modelRunRepository.findById(modelRunId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiên bản mô hình với ID: " + modelRunId));
        return mapToSummaryDto(run);
    }

    @Override
    @Transactional
    public ModelVersionSummaryDto activateModelVersion(Long modelRunId) {
        MiningModelRun target = modelRunRepository.findById(modelRunId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiên bản mô hình với ID: " + modelRunId));

        if (target.getRunStatus() != RunStatus.SUCCESS) {
            throw new IllegalStateException("Chỉ có thể kích hoạt phiên bản có trạng thái huấn luyện thành công (SUCCESS)!");
        }

        // Deactivate model đang active của cùng modelType
        modelRunRepository.deactivateExistingModels(target.getModelType());

        // Kích hoạt model này
        target.setIsActive(1);
        target = modelRunRepository.save(target);

        log.info("Model version activated successfully! ModelRunId: {}, Type: {}", target.getId(), target.getModelType());
        return mapToSummaryDto(target);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelComparisonResponse compareModelVersions(Long sourceId, Long targetId) {
        MiningModelRun source = modelRunRepository.findById(sourceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiên bản gốc với ID: " + sourceId));
        MiningModelRun target = modelRunRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiên bản đối chiếu với ID: " + targetId));

        if (source.getModelType() != target.getModelType()) {
            throw new IllegalArgumentException("Chỉ có thể so sánh giữa hai phiên bản của cùng một loại mô hình (" + source.getModelType() + " vs " + target.getModelType() + ")!");
        }

        ModelVersionSummaryDto sourceDto = mapToSummaryDto(source);
        ModelVersionSummaryDto targetDto = mapToSummaryDto(target);

        // 1. So sánh độ đo toán học (Metrics Diff)
        Map<String, ModelComparisonResponse.MetricDiff> metricsDiff = new LinkedHashMap<>();
        Map<String, Object> sMetrics = sourceDto.getMetrics() != null ? sourceDto.getMetrics() : Collections.emptyMap();
        Map<String, Object> tMetrics = targetDto.getMetrics() != null ? targetDto.getMetrics() : Collections.emptyMap();

        Set<String> allMetricKeys = new LinkedHashSet<>();
        allMetricKeys.addAll(sMetrics.keySet());
        allMetricKeys.addAll(tMetrics.keySet());

        for (String key : allMetricKeys) {
            Double sVal = extractDouble(sMetrics.get(key));
            Double tVal = extractDouble(tMetrics.get(key));
            if (sVal != null && tVal != null) {
                double diff = tVal - sVal;
                double pct = sVal != 0 ? (diff / sVal) * 100.0 : 0.0;
                String eval = evaluateMetricChange(key, diff);
                metricsDiff.put(key, ModelComparisonResponse.MetricDiff.builder()
                        .sourceValue(Math.round(sVal * 10000.0) / 10000.0)
                        .targetValue(Math.round(tVal * 10000.0) / 10000.0)
                        .changeAmount(Math.round(diff * 10000.0) / 10000.0)
                        .changePercentage(Math.round(pct * 100.0) / 100.0)
                        .evaluation(eval)
                        .build());
            }
        }

        // 2. So sánh chuyển dịch phân cụm (Nếu là CUSTOMER_CLUSTERING)
        List<ModelComparisonResponse.ClusterDriftDto> clusterDrifts = new ArrayList<>();
        if (source.getModelType() == ModelType.CUSTOMER_CLUSTERING) {
            clusterDrifts = buildClusterDrifts(sourceId, targetId);
        }

        // 3. So sánh chuyển dịch luật kết hợp (Nếu là ASSOCIATION_RULES)
        ModelComparisonResponse.RuleDriftSummaryDto ruleDriftSummary = null;
        if (source.getModelType() == ModelType.ASSOCIATION_RULES) {
            ruleDriftSummary = buildRuleDriftSummary(sourceId, targetId);
        }

        return ModelComparisonResponse.builder()
                .sourceModel(sourceDto)
                .targetModel(targetDto)
                .metricsDiff(metricsDiff)
                .clusterDrifts(clusterDrifts)
                .ruleDriftSummary(ruleDriftSummary)
                .build();
    }

    private List<ModelComparisonResponse.ClusterDriftDto> buildClusterDrifts(Long sourceRunId, Long targetRunId) {
        List<Object[]> sStats = customerClusterRepository.getClusterStatsByModelRunId(sourceRunId);
        List<Object[]> tStats = customerClusterRepository.getClusterStatsByModelRunId(targetRunId);

        long sTotal = sStats.stream().mapToLong(r -> r[2] != null ? (Long) r[2] : 0L).sum();
        long tTotal = tStats.stream().mapToLong(r -> r[2] != null ? (Long) r[2] : 0L).sum();

        Map<Integer, Object[]> tMap = tStats.stream().collect(Collectors.toMap(r -> (Integer) r[0], r -> r));

        List<ModelComparisonResponse.ClusterDriftDto> drifts = new ArrayList<>();
        for (Object[] sRow : sStats) {
            Integer cId = (Integer) sRow[0];
            String sLabel = (String) sRow[1];
            Long sCount = (Long) sRow[2];
            double sPct = sTotal > 0 ? (sCount * 100.0 / sTotal) : 0.0;

            Object[] tRow = tMap.get(cId);
            String tLabel = tRow != null ? (String) tRow[1] : sLabel;
            Long tCount = tRow != null ? (Long) tRow[2] : 0L;
            double tPct = tTotal > 0 ? (tCount * 100.0 / tTotal) : 0.0;

            double diff = Math.round((tPct - sPct) * 10.0) / 10.0;
            String analysis = diff > 0 ? "Tăng trưởng thị phần nhóm +" + diff + "%" : (diff < 0 ? "Thu hẹp tỷ trọng nhóm " + diff + "%" : "Duy trì ổn định");

            drifts.add(ModelComparisonResponse.ClusterDriftDto.builder()
                    .clusterId(cId)
                    .sourceLabel(sLabel)
                    .targetLabel(tLabel)
                    .sourceCount(sCount.intValue())
                    .targetCount(tCount.intValue())
                    .sourcePercentage(Math.round(sPct * 10.0) / 10.0)
                    .targetPercentage(Math.round(tPct * 10.0) / 10.0)
                    .percentageDiff(diff)
                    .driftAnalysis(analysis)
                    .build());
        }
        return drifts;
    }

    private ModelComparisonResponse.RuleDriftSummaryDto buildRuleDriftSummary(Long sourceRunId, Long targetRunId) {
        List<AssociationRule> sRules = associationRuleRepository.findByModelRunId(sourceRunId);
        List<AssociationRule> tRules = associationRuleRepository.findByModelRunId(targetRunId);

        Map<String, AssociationRule> sMap = sRules.stream()
                .filter(r -> r.getAntecedentsSummary() != null && r.getConsequentsSummary() != null)
                .collect(Collectors.toMap(r -> r.getAntecedentsSummary() + "==>" + r.getConsequentsSummary(), r -> r, (a, b) -> a));

        Map<String, AssociationRule> tMap = tRules.stream()
                .filter(r -> r.getAntecedentsSummary() != null && r.getConsequentsSummary() != null)
                .collect(Collectors.toMap(r -> r.getAntecedentsSummary() + "==>" + r.getConsequentsSummary(), r -> r, (a, b) -> a));

        int commonCount = 0;
        int emergingCount = 0;
        List<AssociationRule> emergingRules = new ArrayList<>();

        for (Map.Entry<String, AssociationRule> entry : tMap.entrySet()) {
            if (sMap.containsKey(entry.getKey())) {
                commonCount++;
            } else {
                emergingCount++;
                emergingRules.add(entry.getValue());
            }
        }
        int droppedCount = sMap.size() - commonCount;

        emergingRules.sort((a, b) -> Double.compare(b.getLift(), a.getLift()));
        List<AssociationRuleDto> topEmerging = emergingRules.stream().limit(5).map(rule -> AssociationRuleDto.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .antecedentCount(rule.getAntecedentCount())
                .consequentCount(rule.getConsequentCount())
                .antecedentsSummary(rule.getAntecedentsSummary())
                .consequentsSummary(rule.getConsequentsSummary())
                .support(rule.getSupport())
                .confidence(rule.getConfidence())
                .lift(rule.getLift())
                .transactionCount(rule.getTransactionCount())
                .isActive(rule.getIsActive())
                .build()).collect(Collectors.toList());

        double sAvgLift = sRules.stream().mapToDouble(AssociationRule::getLift).average().orElse(0.0);
        double tAvgLift = tRules.stream().mapToDouble(AssociationRule::getLift).average().orElse(0.0);

        return ModelComparisonResponse.RuleDriftSummaryDto.builder()
                .sourceTotalRules(sRules.size())
                .targetTotalRules(tRules.size())
                .commonRulesCount(commonCount)
                .emergingRulesCount(emergingCount)
                .droppedRulesCount(Math.max(0, droppedCount))
                .sourceAvgLift(Math.round(sAvgLift * 100.0) / 100.0)
                .targetAvgLift(Math.round(tAvgLift * 100.0) / 100.0)
                .topEmergingRules(topEmerging)
                .build();
    }

    private String evaluateMetricChange(String metricKey, double diff) {
        String keyLower = metricKey.toLowerCase();
        if (Math.abs(diff) < 0.001) return "STABLE";

        // Chỉ số càng lớn càng tốt
        if (keyLower.contains("silhouette") || keyLower.contains("calinski") || keyLower.contains("lift") || keyLower.contains("confidence") || keyLower.contains("support")) {
            return diff > 0 ? "BETTER" : "WORSE";
        }
        // Chỉ số càng nhỏ càng tốt
        if (keyLower.contains("davies") || keyLower.contains("wcss") || keyLower.contains("inertia") || keyLower.contains("memory") || keyLower.contains("time")) {
            return diff < 0 ? "BETTER" : "WORSE";
        }
        return diff > 0 ? "INCREASED" : "DECREASED";
    }

    private Double extractDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return null;
    }

    private ModelVersionSummaryDto mapToSummaryDto(MiningModelRun entity) {
        return ModelVersionSummaryDto.builder()
                .id(entity.getId())
                .modelType(entity.getModelType())
                .algorithmName(entity.getAlgorithmName())
                .runStatus(entity.getRunStatus())
                .isActive(entity.getIsActive())
                .dataOrigin(entity.getDataOrigin())
                .datasetVersion(entity.getDatasetVersion())
                .datasetRowCount(entity.getDatasetRowCount())
                .parameters(parseJsonToMap(entity.getParametersJson()))
                .metrics(parseJsonToMap(entity.getMetricsJson()))
                .notes(entity.getNotes())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .createdDate(entity.getCreatedDate())
                .build();
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON string to Map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private MiningModelRunDto mapToModelRunDto(MiningModelRun entity) {
        return MiningModelRunDto.builder()
                .id(entity.getId())
                .modelType(entity.getModelType())
                .algorithmName(entity.getAlgorithmName())
                .runStatus(entity.getRunStatus())
                .isActive(entity.getIsActive())
                .dataOrigin(entity.getDataOrigin())
                .datasetVersion(entity.getDatasetVersion())
                .datasetRowCount(entity.getDatasetRowCount())
                .parametersJson(entity.getParametersJson())
                .metricsJson(entity.getMetricsJson())
                .notes(entity.getNotes())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .createdDate(entity.getCreatedDate())
                .build();
    }

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON string: {}", e.getMessage());
            return null;
        }
    }
}
