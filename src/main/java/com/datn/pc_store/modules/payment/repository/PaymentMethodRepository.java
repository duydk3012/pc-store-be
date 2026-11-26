package com.datn.pc_store.modules.payment.repository;

import com.datn.pc_store.modules.payment.entity.PaymentMethodEntity;
import com.datn.pc_store.modules.payment.enums.PaymentMethodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

    Optional<PaymentMethodEntity> findByCodeIgnoreCase(String code);

    List<PaymentMethodEntity> findAllByStatusOrderByIdAsc(PaymentMethodStatus status);
}
