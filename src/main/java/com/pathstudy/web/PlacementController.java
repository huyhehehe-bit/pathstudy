package com.pathstudy.web;

import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.PlacementService;
import com.pathstudy.web.dto.PlacementOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/placement/{code}")
public class PlacementController {

    private final SubjectRepository subjects;
    private final PlacementService placement;
    private final CurrentUserService currentUser;

    public PlacementController(SubjectRepository subjects, PlacementService placement,
                               CurrentUserService currentUser) {
        this.subjects = subjects;
        this.placement = placement;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String intro(@PathVariable String code, @RequestParam(required = false) String grade,
                        Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        Subject subject = subjects.findByCode(code).orElse(null);
        if (subject == null || !subject.isActive()) {
            ra.addFlashAttribute("toast", "Môn này chưa có bài kiểm tra đầu vào.");
            return "redirect:/subjects";
        }
        grade = resolveGrade(user, subject, grade);
        if (needsGrade(subject, grade)) {
            return "redirect:/english";
        }
        int used = placement.attemptsUsed(user, subject, grade);
        model.addAttribute("subject", subject);
        model.addAttribute("grade", grade);
        model.addAttribute("attemptsUsed", used);
        model.addAttribute("attemptsMax", PlacementService.ATTEMPTS_MAX);
        model.addAttribute("canAttempt", used < PlacementService.ATTEMPTS_MAX);
        model.addAttribute("questionCount", placement.questionsFor(subject, grade).size());
        return "placement/intro";
    }

    @GetMapping("/test")
    public String test(@PathVariable String code, @RequestParam(required = false) String grade,
                       Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        Subject subject = subjects.findByCode(code).orElseThrow();
        grade = resolveGrade(user, subject, grade);
        if (needsGrade(subject, grade)) {
            return "redirect:/english";
        }
        if (!placement.canAttempt(user, subject, grade)) {
            ra.addFlashAttribute("toast", "Bạn đã dùng hết 3 lần làm bài.");
            return "redirect:/placement/" + code;
        }
        model.addAttribute("subject", subject);
        model.addAttribute("grade", grade);
        model.addAttribute("questions", placement.questionsFor(subject, grade));
        model.addAttribute("attemptNo", placement.attemptsUsed(user, subject, grade) + 1);
        return "placement/test";
    }

    @PostMapping("/submit")
    public String submit(@PathVariable String code, @RequestParam(required = false) String grade,
                         @RequestParam Map<String, String> params, RedirectAttributes ra) {
        User user = currentUser.require();
        Subject subject = subjects.findByCode(code).orElseThrow();
        grade = resolveGrade(user, subject, grade);
        if (needsGrade(subject, grade)) {
            return "redirect:/english";
        }
        if (!placement.canAttempt(user, subject, grade)) {
            ra.addFlashAttribute("toast", "Bạn đã dùng hết 3 lần làm bài.");
            return "redirect:/placement/" + code;
        }
        PlacementOutcome outcome = placement.grade(user, subject, grade, parseAnswers(params));
        ra.addFlashAttribute("outcome", outcome);
        String suffix = grade == null ? "" : "?grade=" + org.springframework.web.util.UriUtils
                .encode(grade, java.nio.charset.StandardCharsets.UTF_8);
        return "redirect:/placement/" + code + "/result" + suffix;
    }

    @GetMapping("/result")
    public String result(@PathVariable String code, @RequestParam(required = false) String grade,
                         Model model) {
        if (!model.containsAttribute("outcome")) {
            return "redirect:/placement/" + code;
        }
        model.addAttribute("grade", grade);
        return "placement/result";
    }

    /**
     * Tiếng Anh là môn theo khối: nếu thiếu grade (đi thẳng từ trang "Học đúng hướng"
     * hoặc grade rỗng ""), lấy khối từ tài khoản. Ngữ văn giữ nguyên grade=null.
     */
    private String resolveGrade(User user, Subject subject, String grade) {
        if (grade != null && grade.isBlank()) {
            grade = null;
        }
        if (grade == null && "anh".equals(subject.getCode()) && user.getGrade() != null) {
            grade = user.getGrade();
        }
        return grade;
    }

    /** Tiếng Anh mà vẫn chưa xác định được khối → phải chọn khối trước (đẩy về /english). */
    private boolean needsGrade(Subject subject, String grade) {
        return "anh".equals(subject.getCode()) && grade == null;
    }

    static Map<Long, Integer> parseAnswers(Map<String, String> params) {
        Map<Long, Integer> answers = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("q_")) {
                try {
                    answers.put(Long.parseLong(e.getKey().substring(2)), Integer.parseInt(e.getValue()));
                } catch (NumberFormatException ignore) {
                    // skip malformed field
                }
            }
        }
        return answers;
    }
}
