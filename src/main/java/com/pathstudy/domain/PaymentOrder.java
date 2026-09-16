package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A premium purchase paid by bank transfer. Each order has a unique memo code
 * (e.g. "PS12") that the buyer includes in the transfer content; a reconciliation
 * service (SePay/Casso) then calls our webhook and we match the memo to this order.
 */
@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User user;

    @Column(nullable = false)
    private String planCode;

    @Column(nullable = false)
    private int amount;

    @Column(unique = true)
    private String memoCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime paidAt;

    private LocalDateTime expiresAt;
}
