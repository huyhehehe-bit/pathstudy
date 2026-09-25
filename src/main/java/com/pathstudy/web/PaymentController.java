package com.pathstudy.web;

import com.pathstudy.domain.PaymentOrder;
import com.pathstudy.domain.PremiumPlan;
import com.pathstudy.domain.User;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentController {

    @Value("${app.payment.bank:MB}")
    private String bank;
    @Value("${app.payment.account:0000000000}")
    private String account;
    @Value("${app.payment.account-name:PATHSTUDY}")
    private String accountName;
    @Value("${app.payment.webhook-token:changeme-webhook-token}")
    private String webhookToken;

    private final BankTransferPaymentService payments;
    private final CurrentUserService currentUser;

    public PaymentController(BankTransferPaymentService payments, CurrentUserService currentUser) {
        this.payments = payments;
        this.currentUser = currentUser;
    }

    @GetMapping("/upgrade")
    public String upgrade(Model model) {
        User user = currentUser.require();
        model.addAttribute("plans", PremiumPlan.values());
        model.addAttribute("subscription", payments.activeSubscription(user).orElse(null));
        return "payment/upgrade";
    }

    @PostMapping("/upgrade/checkout")
    public String checkout(@RequestParam String plan) {
        User user = currentUser.require();
        PaymentOrder order = payments.createOrder(user, PremiumPlan.fromCode(plan));
        return "redirect:/upgrade/order/" + order.getId();
    }

    @GetMapping("/upgrade/order/{id}")
    public String order(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        PaymentOrder order = payments.getOrder(id)
                .filter(o -> o.getUser().getId().equals(user.getId()))
                .orElse(null);
        if (order == null) {
            ra.addFlashAttribute("toast", "Không tìm thấy đơn hàng.");
            return "redirect:/upgrade";
        }
        String qrUrl = "https://img.vietqr.io/image/" + bank + "-" + account + "-compact2.png"
                + "?amount=" + order.getAmount()
                + "&addInfo=" + enc(order.getMemoCode())
                + "&accountName=" + enc(accountName);
        model.addAttribute("order", order);
        model.addAttribute("plan", PremiumPlan.fromCode(order.getPlanCode()));
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("bank", bank);
        model.addAttribute("account", account);
        model.addAttribute("accountName", accountName);
        return "payment/order";
    }

    /**
     * Webhook for a bank-reconciliation service (SePay / Casso). Configure the
     * service to POST here with the shared token. Accepts SePay's flat payload
     * or Casso's {data:[...]} payload.
     */
    private static final ObjectMapper JSON = new ObjectMapper();

    @PostMapping("/payment/webhook")
    public void webhook(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String auth,
            HttpServletResponse response) throws IOException {

        int status;
        Map<String, Object> payload;
        if (!authorized(token, auth)) {
            status = 401;
            payload = Map.of("success", false, "message", "unauthorized");
        } else if (body == null) {
            status = 200;
            payload = Map.of("success", true);
        } else {
            int confirmed = 0;
            Object data = body.get("data");
            if (data instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> tx && handle(tx)) {
                        confirmed++;
                    }
                }
            } else if (handle(body)) {
                confirmed++;
            }
            status = 200;
            payload = Map.of("success", true, "confirmed", confirmed);
        }
        // Ghi JSON thẳng vào response, KHÔNG qua content negotiation — tránh 406
        // (và log rác HttpMessageNotWritableException) khi bot/scanner gọi webhook
        // công khai này với header Accept: text/html.
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(JSON.writeValueAsString(payload));
    }

    private boolean handle(Map<?, ?> tx) {
        Object type = tx.get("transferType");
        if (type != null && !"in".equalsIgnoreCase(String.valueOf(type))) {
            return false; // ignore outgoing transfers
        }
        String content = str(firstNonNull(tx.get("content"), tx.get("description")));
        long amount = toLong(firstNonNull(tx.get("transferAmount"), tx.get("amount")));
        return payments.confirmByTransfer(content, amount);
    }

    private boolean authorized(String token, String auth) {
        if (webhookToken == null || webhookToken.isBlank()) {
            return false;
        }
        if (webhookToken.equals(token)) {
            return true;
        }
        return auth != null && auth.contains(webhookToken);
    }

    private static Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long toLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.longValue();
        try {
            return (long) Double.parseDouble(String.valueOf(o).replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
