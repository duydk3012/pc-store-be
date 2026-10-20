package com.datn.pc_store.modules.category.repository;

import com.datn.pc_store.modules.category.entity.Category;
import com.datn.pc_store.modules.category.enums.CategoryStatus;
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
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByParentId(Long parentId);

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNull();

    List<Category> findByParentIsNullAndStatus(CategoryStatus status);

    List<Category> findByParentId(Long parentId);

    List<Category> findByParentIdAndStatus(Long parentId, CategoryStatus status);

    @Query("""
        SELECT c FROM Category c
        WHERE (:status IS NULL OR c.status = :status)
          AND (:parentId IS NULL OR c.parent.id = :parentId)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Category> searchCategories(
            @Param("keyword") String keyword,
            @Param("status") CategoryStatus status,
            @Param("parentId") Long parentId,
            Pageable pageable
    );
}
