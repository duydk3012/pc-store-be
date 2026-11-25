package com.datn.pc_store.modules.recommendation.repository;

import com.datn.pc_store.modules.recommendation.entity.MiningModelRun;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiningModelRunRepository extends JpaRepository<MiningModelRun, Long> {

    Optional<MiningModelRun> findFirstByModelTypeAndIsActiveOrderByCreatedDateDesc(ModelType modelType, Integer isActive);

    List<MiningModelRun> findAllByOrderByCreatedDateDesc();

    Page<MiningModelRun> findAllByOrderByCreatedDateDesc(Pageable pageable);

    Page<MiningModelRun> findByModelTypeOrderByCreatedDateDesc(ModelType modelType, Pageable pageable);

    @Modifying
    @Query("UPDATE MiningModelRun mr SET mr.isActive = 0 WHERE mr.modelType = :modelType AND mr.isActive = 1")
    int deactivateExistingModels(@Param("modelType") ModelType modelType);
}
