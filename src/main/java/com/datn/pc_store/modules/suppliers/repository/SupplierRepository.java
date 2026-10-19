package com.datn.pc_store.modules.suppliers.repository;

import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Supplier> findByCode(String code);

    @Query("""
        SELECT s FROM Supplier s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.taxCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Supplier> searchSuppliers(
            @Param("keyword") String keyword,
            @Param("status") SupplierStatus status,
            Pageable pageable
    );
}
