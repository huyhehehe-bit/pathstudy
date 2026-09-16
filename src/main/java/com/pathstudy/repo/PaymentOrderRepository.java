package com.pathstudy.repo;

import com.pathstudy.domain.OrderStatus;
import com.pathstudy.domain.PaymentOrder;
import com.pathstudy.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByMemoCode(String memoCode);
    boolean existsByUserAndStatusAndExpiresAtAfter(User user, OrderStatus status, LocalDateTime when);
    Optional<PaymentOrder> findTopByUserAndStatusOrderByExpiresAtDesc(User user, OrderStatus status);
}
