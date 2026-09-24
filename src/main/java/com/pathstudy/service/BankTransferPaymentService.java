package com.pathstudy.service;

import com.pathstudy.domain.OrderStatus;
import com.pathstudy.domain.PaymentOrder;
import com.pathstudy.domain.PremiumPlan;
import com.pathstudy.domain.User;
import com.pathstudy.repo.PaymentOrderRepository;
import com.pathstudy.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bank-transfer implementation of {@link PaymentService}. No payment gateway:
 * the buyer transfers to a personal bank account with a unique memo, and a
 * reconciliation service (SePay/Casso) calls our webhook, which routes here to
 * mark the matching order paid and grant premium.
 */
@Service
public class BankTransferPaymentService implements PaymentService {

    private static final Pattern MEMO = Pattern.compile("PS(\\d+)");

    private final PaymentOrderRepository orders;
    private final UserRepository users;

    public BankTransferPaymentService(PaymentOrderRepository orders, UserRepository users) {
        this.orders = orders;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPremiumAccess(Long userId) {
        return users.findById(userId)
                .map(u -> orders.existsByUserAndStatusAndExpiresAtAfter(u, OrderStatus.PAID, LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasPremiumAccess(User user) {
        return orders.existsByUserAndStatusAndExpiresAtAfter(user, OrderStatus.PAID, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<PaymentOrder> activeSubscription(User user) {
        return orders.findTopByUserAndStatusOrderByExpiresAtDesc(user, OrderStatus.PAID)
                .filter(o -> o.getExpiresAt() != null && o.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /** Admin cấp Premium miễn phí (comp) cho user trong {@code days} ngày. */
    @Transactional
    public void grantComp(User user, int days) {
        PaymentOrder order = new PaymentOrder();
        order.setUser(user);
        order.setPlanCode("ADMIN_COMP");
        order.setAmount(0);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setExpiresAt(LocalDateTime.now().plusDays(days));
        orders.save(order);
        order.setMemoCode("COMP" + order.getId());
        orders.save(order);
    }

    /** Admin thu hồi toàn bộ Premium đang có của user (huỷ các đơn PAID). */
    @Transactional
    public void revokePremium(User user) {
        for (PaymentOrder o : orders.findByUserAndStatus(user, OrderStatus.PAID)) {
            o.setStatus(OrderStatus.CANCELLED);
            orders.save(o);
        }
    }

    @Override
    @Transactional
    public String startCheckout(Long userId, String planCode) {
        User user = users.findById(userId).orElseThrow();
        PaymentOrder order = createOrder(user, PremiumPlan.fromCode(planCode));
        return "/upgrade/order/" + order.getId();
    }

    @Transactional
    public PaymentOrder createOrder(User user, PremiumPlan plan) {
        PaymentOrder order = new PaymentOrder();
        order.setUser(user);
        order.setPlanCode(plan.name());
        order.setAmount(plan.getAmount());
        order.setStatus(OrderStatus.PENDING);
        orders.save(order);
        order.setMemoCode("PS" + order.getId());
        return orders.save(order);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentOrder> getOrder(Long id) {
        return orders.findById(id);
    }

    /**
     * Called from the webhook. Extracts the memo (PS&lt;id&gt;) from the transfer
     * content and, if the amount covers the order, marks it paid and grants premium.
     * @return true if an order was (or already had been) confirmed.
     */
    @Transactional
    public boolean confirmByTransfer(String content, long amount) {
        if (content == null) {
            return false;
        }
        Matcher m = MEMO.matcher(content.toUpperCase());
        if (!m.find()) {
            return false;
        }
        String memo = "PS" + m.group(1);
        Optional<PaymentOrder> opt = orders.findByMemoCode(memo);
        if (opt.isEmpty()) {
            return false;
        }
        PaymentOrder order = opt.get();
        if (order.getStatus() == OrderStatus.PAID) {
            return true;
        }
        if (amount < order.getAmount()) {
            return false; // underpaid — leave pending
        }
        PremiumPlan plan = PremiumPlan.fromCode(order.getPlanCode());
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        order.setExpiresAt(now.plusDays(plan.getDays()));
        orders.save(order);
        return true;
    }
}
