package com.datn.pc_store.modules.recommendation.repository;

import com.datn.pc_store.modules.recommendation.entity.CustomerCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerClusterRepository extends JpaRepository<CustomerCluster, Long> {

    @Query("""
        SELECT cc FROM CustomerCluster cc
        JOIN cc.modelRun mr
        WHERE cc.customer.id = :customerId
          AND mr.isActive = 1
    """)
    Optional<CustomerCluster> findActiveByCustomerId(@Param("customerId") Long customerId);

    @Query("""
        SELECT cc.clusterId, cc.clusterLabel, COUNT(cc.id), AVG(cc.distanceToCentroid)
        FROM CustomerCluster cc
        JOIN cc.modelRun mr
        WHERE mr.isActive = 1
        GROUP BY cc.clusterId, cc.clusterLabel
        ORDER BY cc.clusterId ASC
    """)
    List<Object[]> getClusterStatsFromActiveModel();

    @Query("""
        SELECT cc.clusterId, cc.clusterLabel, COUNT(cc.id), AVG(cc.distanceToCentroid)
        FROM CustomerCluster cc
        WHERE cc.modelRun.id = :modelRunId
        GROUP BY cc.clusterId, cc.clusterLabel
        ORDER BY cc.clusterId ASC
    """)
    List<Object[]> getClusterStatsByModelRunId(@Param("modelRunId") Long modelRunId);
}
