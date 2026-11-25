package com.datn.pc_store.modules.recommendation.entity;

import com.datn.pc_store.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "customer_clusters", uniqueConstraints = {
    @UniqueConstraint(name = "uk_customer_cluster_active", columnNames = {"customer_id", "model_run_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "cluster_id", nullable = false)
    private Integer clusterId;

    @Column(name = "cluster_label", nullable = false, length = 100)
    private String clusterLabel;

    @Column(name = "distance_to_centroid")
    private Double distanceToCentroid;

    @Column(name = "features_json", columnDefinition = "JSON")
    private String featuresJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_run_id", nullable = false)
    private MiningModelRun modelRun;

    @CreatedDate
    @Column(name = "assigned_at", updatable = false, nullable = false)
    private Instant assignedAt;
}
