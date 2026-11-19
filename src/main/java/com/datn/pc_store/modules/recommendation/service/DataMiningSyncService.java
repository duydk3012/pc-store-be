package com.datn.pc_store.modules.recommendation.service;

import com.datn.pc_store.modules.recommendation.dto.request.SyncMiningPayloadRequest;
import com.datn.pc_store.modules.recommendation.dto.response.*;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DataMiningSyncService {

    MiningModelRunDto syncModelData(SyncMiningPayloadRequest request);

    List<MiningModelRunDto> getAllModelRuns();

    MiningModelRunDto getLatestActiveModel(ModelType modelType);

    List<CustomerClusterDto> getClusterSummaries();

    Page<AssociationRuleDto> getActiveAssociationRules(Double minLift, Double minConfidence, Pageable pageable);

    // BƯỚC 2: QUẢN LÝ PHIÊN BẢN, ROLLBACK SWITCHER & SO SÁNH CHUYỂN DỊCH
    Page<ModelVersionSummaryDto> getModelHistory(ModelType modelType, Pageable pageable);

    ModelVersionSummaryDto getModelDetails(Long modelRunId);

    ModelVersionSummaryDto activateModelVersion(Long modelRunId);

    ModelComparisonResponse compareModelVersions(Long sourceId, Long targetId);
}
