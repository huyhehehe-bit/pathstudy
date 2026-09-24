package com.pathstudy.web;

import com.pathstudy.domain.User;
import com.pathstudy.repo.UserRepository;
import com.pathstudy.service.AiStudyPlanService;
import com.pathstudy.service.BankTransferPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private static final List<String> ROLES = List.of("STUDENT", "TEACHER", "ADMIN");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserRepository users;
    private final AiStudyPlanService aiStudyPlan;
    private final BankTransferPaymentService payments;

    public AdminController(UserRepository users, AiStudyPlanService aiStudyPlan,
                           BankTransferPaymentService payments) {
        this.users = users;
        this.aiStudyPlan = aiStudyPlan;
        this.payments = payments;
    }

    /** Admin-only AI health check (see SecurityConfig: /admin/** requires ADMIN). */
    @GetMapping(value = "/admin/ai-check", produces = "text/plain; charset=UTF-8")
    @ResponseBody
    public String aiCheck() {
        return aiStudyPlan.diagnose();
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        List<User> all = users.findAll();
        // userId → hạn Premium (dd/MM/yyyy) nếu đang có, ngược lại null (Free).
        Map<Long, String> premiumUntil = new LinkedHashMap<>();
        for (User u : all) {
            premiumUntil.put(u.getId(), payments.activeSubscription(u)
                    .map(o -> o.getExpiresAt().format(DF)).orElse(null));
        }
        model.addAttribute("users", all);
        model.addAttribute("roles", ROLES);
        model.addAttribute("premiumUntil", premiumUntil);
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role, RedirectAttributes ra) {
        if (!ROLES.contains(role)) {
            ra.addFlashAttribute("toast", "Vai trò không hợp lệ.");
            return "redirect:/admin/users";
        }
        users.findById(id).ifPresent(u -> {
            u.setRole(role);
            users.save(u);
        });
        ra.addFlashAttribute("toast", "Đã cập nhật vai trò.");
        return "redirect:/admin/users";
    }

    /** Admin mở Premium miễn phí cho user trong {@code days} ngày (mặc định 30). */
    @PostMapping("/admin/users/{id}/premium/grant")
    public String grantPremium(@PathVariable Long id,
                               @RequestParam(defaultValue = "30") int days,
                               RedirectAttributes ra) {
        if (days < 1 || days > 3650) {
            ra.addFlashAttribute("toast", "Số ngày không hợp lệ (1–3650).");
            return "redirect:/admin/users";
        }
        users.findById(id).ifPresentOrElse(u -> {
            payments.grantComp(u, days);
            ra.addFlashAttribute("toast", "Đã mở Premium " + days + " ngày cho " + u.getEmail() + ".");
        }, () -> ra.addFlashAttribute("toast", "Không tìm thấy người dùng."));
        return "redirect:/admin/users";
    }

    /** Admin thu hồi Premium của user (huỷ các đơn PAID). */
    @PostMapping("/admin/users/{id}/premium/revoke")
    public String revokePremium(@PathVariable Long id, RedirectAttributes ra) {
        users.findById(id).ifPresentOrElse(u -> {
            payments.revokePremium(u);
            ra.addFlashAttribute("toast", "Đã thu hồi Premium của " + u.getEmail() + ".");
        }, () -> ra.addFlashAttribute("toast", "Không tìm thấy người dùng."));
        return "redirect:/admin/users";
    }
}
