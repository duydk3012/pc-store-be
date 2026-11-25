package com.datn.pc_store.modules.cart.repository;

import com.datn.pc_store.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCart_IdAndProductId(Long cartId, Long productId);

    List<CartItem> findAllByCart_IdOrderByIdAsc(Long cartId);
}
