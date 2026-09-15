package com.pathstudy.service;

/**
 * Payment integration is intentionally left unimplemented.
 * <p>
 * The founder will implement this later (VNPay / MoMo / Stripe / bank transfer, etc.).
 * Wire a concrete {@code @Service} implementation of this interface and inject it
 * where premium gating is needed. Nothing else in the app depends on it yet.
 */
public interface PaymentService {

    /** Whether the given user currently has premium access. */
    boolean hasPremiumAccess(Long userId);

    /**
     * Start a checkout for a plan and return a redirect URL to the payment gateway.
     * @throws UnsupportedOperationException until implemented.
     */
    String startCheckout(Long userId, String planCode);
}
