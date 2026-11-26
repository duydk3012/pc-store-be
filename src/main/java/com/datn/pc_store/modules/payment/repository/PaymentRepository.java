package com.datn.pc_store.modules.payment.repository;

import com.datn.pc_store.modules.payment.entity.Payment;
import com.datn.pc_store.modules.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    List<Payment> findAllByOrderIdOrderByIdDesc(Long orderId);

    List<Payment> findAllByOrderIdInOrderByIdDesc(Collection<Long> orderIds);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByTransactionCode(String transactionCode);
}
