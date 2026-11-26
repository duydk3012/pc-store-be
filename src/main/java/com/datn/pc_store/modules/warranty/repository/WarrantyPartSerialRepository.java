package com.datn.pc_store.modules.warranty.repository;

import com.datn.pc_store.modules.warranty.entity.WarrantyPartSerial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarrantyPartSerialRepository extends JpaRepository<WarrantyPartSerial, Long> {
    List<WarrantyPartSerial> findByWarrantyPartId(Long warrantyPartId);
    List<WarrantyPartSerial> findByWarrantyPartIdIn(Collection<Long> warrantyPartIds);
    Optional<WarrantyPartSerial> findByWarrantyPartIdAndProductSerialId(Long warrantyPartId, Long productSerialId);
    void deleteByWarrantyPartId(Long warrantyPartId);
    void deleteByWarrantyPartIdAndProductSerialId(Long warrantyPartId, Long productSerialId);
    void deleteByWarrantyPartIdIn(Collection<Long> warrantyPartIds);
}
