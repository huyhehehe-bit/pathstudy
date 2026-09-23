package com.pathstudy.web;

import com.pathstudy.domain.User;
import com.pathstudy.repo.UserRepository;
import com.pathstudy.service.AiStudyPlanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private static final List<String> ROLES = List.of("STUDENT", "TEACHER", "ADMIN");

    private final UserRepository users;
    private final AiStudyPlanService aiStudyPlan;

    public AdminController(UserRepository users, AiStudyPlanService aiStudyPlan) {
        this.users = users;
        this.aiStudyPlan = aiStudyPlan;
    }

    /** Admin-only AI health check (see SecurityConfig: /admin/** requires ADMIN). */
    @GetMapping(value = "/admin/ai-check", produces = "text/plain; charset=UTF-8")
    @ResponseBody
    public String aiCheck() {
        return aiStudyPlan.diagnose();
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", users.findAll());
        model.addAttribute("roles", ROLES);
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
}
