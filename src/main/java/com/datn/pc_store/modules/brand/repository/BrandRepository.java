package com.datn.pc_store.modules.brand.repository;

import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.enums.BrandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>, JpaSpecificationExecutor<Brand> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Brand> findBySlug(String slug);

    List<Brand> findByStatus(BrandStatus status);

    @Query("""
        SELECT b FROM Brand b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(b.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Brand> searchBrands(
            @Param("keyword") String keyword,
            @Param("status") BrandStatus status,
            Pageable pageable
    );
}
