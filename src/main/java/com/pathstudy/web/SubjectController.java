package com.pathstudy.web;

import com.pathstudy.domain.Enrollment;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.StudyPathService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class SubjectController {

    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;
    private final CurrentUserService currentUser;
    private final StudyPathService studyPath;

    public SubjectController(SubjectRepository subjects, EnrollmentRepository enrollments,
                             CurrentUserService currentUser, StudyPathService studyPath) {
        this.subjects = subjects;
        this.enrollments = enrollments;
        this.currentUser = currentUser;
        this.studyPath = studyPath;
    }

    @GetMapping("/subjects")
    public String choose(Model model) {
        User user = currentUser.require();
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("enrolledCodes", enrolledCodes(user));
        model.addAttribute("firstChoice", enrollments.countByUser(user) == 0);
        return "subjects/choose";
    }

    @PostMapping("/subjects/choose")
    public String pick(@RequestParam String code, RedirectAttributes ra) {
        User user = currentUser.require();
        Subject subject = subjects.findByCode(code).orElse(null);
        if (subject == null || !subject.isActive()) {
            ra.addFlashAttribute("toast", "Môn này đang được phát triển, hãy chọn Ngữ văn để trải nghiệm.");
            return "redirect:/subjects";
        }
        boolean primary = enrollments.countByUser(user) == 0;
        studyPath.enroll(user, subject, primary);
        return "redirect:/placement/" + subject.getCode();
    }

    @GetMapping("/subjects/add")
    public String addForm(Model model) {
        User user = currentUser.require();
        Set<String> enrolled = enrolledCodes(user);
        List<Subject> available = subjects.findAllByOrderByOrderIndexAsc().stream()
                .filter(s -> !enrolled.contains(s.getCode()))
                .toList();
        model.addAttribute("subjects", available);
        model.addAttribute("enrolledCount", enrolled.size());
        return "subjects/add";
    }

    @PostMapping("/subjects/add")
    public String add(@RequestParam String code, RedirectAttributes ra) {
        User user = currentUser.require();
        Subject subject = subjects.findByCode(code).orElse(null);
        if (subject == null || !subject.isActive()) {
            ra.addFlashAttribute("toast", "Môn này đang được phát triển và sẽ sớm có mặt.");
            return "redirect:/subjects/add";
        }
        studyPath.enroll(user, subject, false);
        return "redirect:/placement/" + subject.getCode();
    }

    private Set<String> enrolledCodes(User user) {
        Set<String> codes = new HashSet<>();
        for (Enrollment e : enrollments.findByUserOrderByCreatedAtAsc(user)) {
            codes.add(e.getSubject().getCode());
        }
        return codes;
    }
}
