package com.pathstudy.web;

import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.repo.UserRepository;
import com.pathstudy.service.AiStudyPlanService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.PlacementService;
import com.pathstudy.web.dto.PlacementOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/placement/{code}")
public class PlacementController {

    /** Môn có ngân hàng câu hỏi tách theo khối — phải biết khối TRƯỚC khi làm bài,
        nếu không sẽ gộp đề cả 3 khối vào một lần test. */
    private static final Set<String> GRADE_SCOPED = Set.of("anh", "toan");
    private static final List<String> GRADES = List.of("Lớp 10", "Lớp 11", "Lớp 12");

    private final SubjectRepository subjects;
    private final PlacementService placement;
    private final CurrentUserService currentUser;
    private final UserRepository users;
    private final AiStudyPlanService ai;

    public PlacementController(SubjectRepository subjects, PlacementService placement,
                               CurrentUserService currentUser, UserRepository users,
                               AiStudyPlanService ai) {
        this.subjects = subjects;
        this.placement = placement;
        this.currentUser = currentUser;
        this.users = users;
        this.ai = ai;
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
            return gradeGate(code);
        }
        int used = placement.attemptsUsed(user, subject, grade);
        model.addAttribute("subject", subject);
        model.addAttribute("grade", grade);
        model.addAttribute("attemptsUsed", used);
        model.addAttribute("attemptsMax", PlacementService.ATTEMPTS_MAX);
        model.addAttribute("canAttempt", used < PlacementService.ATTEMPTS_MAX);
        model.addAttribute("questionCount", placement.testQuestionCount(subject, grade));
        return "placement/intro";
    }

    @GetMapping("/test")
    public String test(@PathVariable String code, @RequestParam(required = false) String grade,
                       Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        Subject subject = subjects.findByCode(code).orElseThrow();
        grade = resolveGrade(user, subject, grade);
        if (needsGrade(subject, grade)) {
            return gradeGate(code);
        }
        if (!placement.canAttempt(user, subject, grade)) {
            ra.addFlashAttribute("toast", "Bạn đã dùng hết 3 lần làm bài.");
            return "redirect:/placement/" + code;
        }
        model.addAttribute("subject", subject);
        model.addAttribute("grade", grade);
        model.addAttribute("questions", placement.randomTestFor(subject, grade));
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
            return gradeGate(code);
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
        model.addAttribute("aiEnabled", ai.isEnabled());
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
        // Môn có đề theo khối (Tiếng Anh, Toán): lấy khối từ tài khoản nếu thiếu.
        if (grade == null && user.getGrade() != null
                && ("anh".equals(subject.getCode()) || "toan".equals(subject.getCode()))) {
            grade = user.getGrade();
        }
        return grade;
    }

    /** Môn theo khối mà vẫn chưa biết khối → phải chọn khối trước khi làm bài. */
    private boolean needsGrade(Subject subject, String grade) {
        return GRADE_SCOPED.contains(subject.getCode()) && grade == null;
    }

    /** Nơi đưa học sinh tới để chọn khối. Tiếng Anh đã có hub riêng lo việc này. */
    private String gradeGate(String code) {
        return "anh".equals(code) ? "redirect:/english" : "redirect:/placement/" + code + "/grade";
    }

    /** Chọn khối cho môn theo khối (tài khoản chưa có khối). */
    @GetMapping("/grade")
    public String chooseGrade(@PathVariable String code, Model model, RedirectAttributes ra) {
        Subject subject = subjects.findByCode(code).orElse(null);
        if (subject == null || !subject.isActive() || !GRADE_SCOPED.contains(code)) {
            ra.addFlashAttribute("toast", "Môn này không chia theo khối.");
            return "redirect:/subjects";
        }
        model.addAttribute("subject", subject);
        model.addAttribute("grades", GRADES);
        return "placement/choose-grade";
    }

    @PostMapping("/grade")
    public String setGrade(@PathVariable String code, @RequestParam String grade) {
        User user = currentUser.require();
        if (GRADES.contains(grade)) {
            user.setGrade(grade);
            users.save(user);
        }
        return "redirect:/placement/" + code;
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
