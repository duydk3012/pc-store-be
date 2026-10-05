package com.datn.pc_store.product.repository;

import com.datn.pc_store.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByIsDeletedOrderByIdAsc(Integer isDeleted);

    Optional<Product> findByIdAndIsDeleted(Long id, Integer isDeleted);
}
