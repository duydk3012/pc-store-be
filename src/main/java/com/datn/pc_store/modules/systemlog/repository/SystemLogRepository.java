package com.datn.pc_store.modules.systemlog.repository;

import com.datn.pc_store.modules.systemlog.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {

    Page<SystemLog> findByResourceTypeIgnoreCaseAndResourceId(
            String resourceType,
            Long resourceId,
            Pageable pageable
    );

    @Query("""
            SELECT s.status, COUNT(s)
            FROM SystemLog s
            WHERE (:fromDate IS NULL OR s.createdDate >= :fromDate)
              AND (:toDate IS NULL OR s.createdDate <= :toDate)
            GROUP BY s.status
            """)
    List<Object[]> countGroupedByStatus(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("""
            SELECT s.module, COUNT(s)
            FROM SystemLog s
            WHERE (:fromDate IS NULL OR s.createdDate >= :fromDate)
              AND (:toDate IS NULL OR s.createdDate <= :toDate)
            GROUP BY s.module
            """)
    List<Object[]> countGroupedByModule(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("""
            SELECT s.action, COUNT(s)
            FROM SystemLog s
            WHERE (:fromDate IS NULL OR s.createdDate >= :fromDate)
              AND (:toDate IS NULL OR s.createdDate <= :toDate)
            GROUP BY s.action
            """)
    List<Object[]> countGroupedByAction(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );
}
