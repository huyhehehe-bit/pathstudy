package com.pathstudy.web;

import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HomeController {

    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;
    private final CurrentUserService currentUser;

    public HomeController(SubjectRepository subjects, EnrollmentRepository enrollments,
                          CurrentUserService currentUser) {
        this.subjects = subjects;
        this.enrollments = enrollments;
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String landing(Model model) {
        // Already signed in? Go straight to the app instead of the marketing page.
        if (currentUser.current().isPresent()) {
            return "redirect:/start";
        }
        List<Subject> list = subjects.findAllByOrderByOrderIndexAsc();
        model.addAttribute("subjects", list);
        model.addAttribute("goals", StudyGoal.ALL);
        model.addAttribute("blurbs", StudyGoal.BLURB);
        model.addAttribute("goalIcons", StudyGoal.ICON);
        return "landing";
    }

    /** Post-login dispatcher: staff go to their tools, students to their path. */
    /**
     * Cửa vào sau khi đăng nhập, và cũng là nơi SecurityConfig đẩy người dùng
     * tới khi họ vào nhầm khu vực của vai trò khác ({@code ?denied}).
     */
    @GetMapping("/start")
    public String start(@RequestParam(required = false) String denied, RedirectAttributes ra) {
        User user = currentUser.require();
        boolean giaoVien = "TEACHER".equals(user.getRole());
        boolean quanTri = "ADMIN".equals(user.getRole());
        if (denied != null) {
            ra.addFlashAttribute("toast", giaoVien || quanTri
                    ? "Phần đó dành cho học sinh. Tài khoản giáo viên/quản trị không làm bài kiểm tra."
                    : "Bạn không có quyền vào phần đó.");
        }
        if (quanTri) {
            return "redirect:/admin/dashboard";
        }
        if (giaoVien) {
            return "redirect:/teacher";
        }
        long count = enrollments.countByUser(user);
        return count == 0 ? "redirect:/subjects" : "redirect:/path";
    }
}
