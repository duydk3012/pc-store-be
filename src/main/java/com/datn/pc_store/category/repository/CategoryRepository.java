package com.datn.pc_store.category.repository;

import com.datn.pc_store.category.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByIsDeletedOrderByIdAsc(Integer isDeleted);

    Optional<Category> findByIdAndIsDeleted(Long id, Integer isDeleted);
}
