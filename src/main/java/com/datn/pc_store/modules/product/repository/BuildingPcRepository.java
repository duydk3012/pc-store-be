package com.datn.pc_store.modules.product.repository;

import com.datn.pc_store.modules.product.entity.BuildingPc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BuildingPcRepository extends JpaRepository<BuildingPc, Long> {
    List<BuildingPc> findByPcId(Long pcId);

    List<BuildingPc> findByPcIdIn(Collection<Long> pcIds);

    @Modifying
    @Query("DELETE FROM BuildingPc b WHERE b.pcId = :pcId")
    void deleteByPcId(@Param("pcId") Long pcId);
}
