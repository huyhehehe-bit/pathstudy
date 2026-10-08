package com.pathstudy.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathstudy.domain.Feedback;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.repo.UserRepository;
import com.pathstudy.service.AiStudyPlanService;
import com.pathstudy.service.BackupService;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.FeedbackService;
import com.pathstudy.service.KpiService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private static final List<String> ROLES = List.of("STUDENT", "TEACHER", "ADMIN");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final UserRepository users;
    private final AiStudyPlanService aiStudyPlan;
    private final BankTransferPaymentService payments;
    private final FeedbackService feedback;
    private final BackupService backup;
    private final KpiService kpi;
    private final SubjectRepository subjects;

    public AdminController(UserRepository users, AiStudyPlanService aiStudyPlan,
                           BankTransferPaymentService payments, FeedbackService feedback,
                           BackupService backup, KpiService kpi, SubjectRepository subjects) {
        this.users = users;
        this.aiStudyPlan = aiStudyPlan;
        this.payments = payments;
        this.feedback = feedback;
        this.backup = backup;
        this.kpi = kpi;
        this.subjects = subjects;
    }

    /** Quản lý môn học: bật/tắt để mở môn cho học sinh vào học. */
    @GetMapping("/admin/subjects")
    public String subjects(Model model) {
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        return "admin/subjects";
    }

    @PostMapping("/admin/subjects/{id}/toggle")
    public String toggleSubject(@PathVariable Long id, RedirectAttributes ra) {
        subjects.findById(id).ifPresent(s -> {
            s.setActive(!s.isActive());
            subjects.save(s);
            ra.addFlashAttribute("toast", s.isActive()
                    ? "Đã MỞ môn " + s.getName() + " cho học sinh."
                    : "Đã ẩn môn " + s.getName() + ".");
        });
        return "redirect:/admin/subjects";
    }

    /** Dashboard KPI nội bộ (người dùng, Premium, doanh thu, nội dung, feedback). */
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("k", kpi.metrics());
        return "admin/dashboard";
    }

    /** Tải bản sao dữ liệu quan trọng (JSON) về máy. Admin-only (/admin/**). */
    @GetMapping("/admin/backup")
    public void backup(HttpServletResponse response) throws IOException {
        String fileName = "pathstudy-backup-" + LocalDate.now() + ".json";
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        JSON.writerWithDefaultPrettyPrinter().writeValue(response.getWriter(), backup.export());
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

    /** Admin xem toàn bộ đánh giá/góp ý của người dùng + thống kê nhanh. */
    @GetMapping("/admin/feedback")
    public String feedback(Model model) {
        List<Feedback> all = feedback.all();
        long rated = all.stream().filter(f -> f.getRating() != null).count();
        double avg = all.stream().filter(f -> f.getRating() != null)
                .mapToInt(Feedback::getRating).average().orElse(0);
        model.addAttribute("feedbacks", all);
        model.addAttribute("total", all.size());
        model.addAttribute("ratedCount", rated);
        model.addAttribute("avgRating", Math.round(avg * 10) / 10.0);
        return "admin/feedback";
    }
}
