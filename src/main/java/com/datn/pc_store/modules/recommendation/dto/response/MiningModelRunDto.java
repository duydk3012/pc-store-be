package com.datn.pc_store.modules.recommendation.dto.response;

import com.datn.pc_store.modules.recommendation.enums.DataOrigin;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import com.datn.pc_store.modules.recommendation.enums.RunStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiningModelRunDto {
    private Long id;
    private ModelType modelType;
    private String algorithmName;
    private RunStatus runStatus;
    private Integer isActive;
    private DataOrigin dataOrigin;
    private String datasetVersion;
    private Integer datasetRowCount;
    private String parametersJson;
    private String metricsJson;
    private String notes;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdDate;
}
