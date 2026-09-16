package com.pathstudy.web;

import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        List<Subject> list = subjects.findAllByOrderByOrderIndexAsc();
        model.addAttribute("subjects", list);
        return "landing";
    }

    /** Post-login dispatcher: staff go to their tools, students to their path. */
    @GetMapping("/start")
    public String start() {
        User user = currentUser.require();
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/users";
        }
        if ("TEACHER".equals(user.getRole())) {
            return "redirect:/teacher";
        }
        long count = enrollments.countByUser(user);
        return count == 0 ? "redirect:/subjects" : "redirect:/path";
    }
}
