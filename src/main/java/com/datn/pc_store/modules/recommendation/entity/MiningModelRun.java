package com.datn.pc_store.modules.recommendation.entity;

import com.datn.pc_store.modules.recommendation.enums.DataOrigin;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import com.datn.pc_store.modules.recommendation.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "mining_model_runs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiningModelRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 50)
    private ModelType modelType;

    @Column(name = "algorithm_name", nullable = false, length = 100)
    private String algorithmName;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_status", nullable = false, length = 20)
    @Builder.Default
    private RunStatus runStatus = RunStatus.RUNNING;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Integer isActive = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_origin", nullable = false, length = 20)
    @Builder.Default
    private DataOrigin dataOrigin = DataOrigin.REAL;

    @Column(name = "dataset_version", length = 100)
    private String datasetVersion;

    @Column(name = "dataset_row_count", nullable = false)
    @Builder.Default
    private Integer datasetRowCount = 0;

    @Column(name = "parameters_json", columnDefinition = "JSON")
    private String parametersJson;

    @Column(name = "metrics_json", columnDefinition = "JSON")
    private String metricsJson;

    @Column(length = 500)
    private String notes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    private Instant createdDate;
}
