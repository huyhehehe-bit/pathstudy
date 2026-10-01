package com.pathstudy.web;

import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.ExamService;
import com.pathstudy.web.dto.ExamOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Học sinh làm đề thi cho MỌI môn (đề seed hoặc do giáo viên tạo). Chấm điểm và
 * chỉ điểm yếu dùng {@link ExamService#grade}. Đề THPT (category=THPT) miễn phí;
 * đề premium cần gói Premium.
 */
@Controller
public class ExamController {

    private final ExamService examService;
    private final BankTransferPaymentService payments;
    private final CurrentUserService currentUser;
    private final SubjectRepository subjects;

    public ExamController(ExamService examService, BankTransferPaymentService payments,
                          CurrentUserService currentUser, SubjectRepository subjects) {
        this.examService = examService;
        this.payments = payments;
        this.currentUser = currentUser;
        this.subjects = subjects;
    }

    /**
     * Trang "Đề thi" cho học sinh: gom đề theo TỪNG MÔN có đề khớp khối của học
     * sinh (không cần đăng ký môn) — đề luyện tập (khớp khối hoặc đề chung) + đề
     * THPT Quốc gia. Bấm vào là làm (/exam/{id}).
     */
    @GetMapping("/exams")
    public String list(Model model) {
        User user = currentUser.require();
        String grade = user.getGrade();
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Subject s : subjects.findAllByOrderByOrderIndexAsc()) {
            List<Exam> practice = examService.listExams(s, grade);
            List<Exam> national = examService.listNationalExams(s);
            if (practice.isEmpty() && national.isEmpty()) {
                continue;
            }
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("subject", s);
            g.put("practice", practice);
            g.put("national", national);
            groups.add(g);
        }
        model.addAttribute("groups", groups);
        model.addAttribute("grade", grade);
        model.addAttribute("isPremiumUser", payments.hasPremiumAccess(user));
        return "exam/list";
    }

    /** true nếu được phép làm đề (THPT miễn phí, hoặc không premium, hoặc có Premium). */
    private boolean allowed(Exam exam, User user) {
        boolean national = "THPT".equals(exam.getCategory());
        return national || !exam.isPremium() || payments.hasPremiumAccess(user);
    }

    @GetMapping("/exam/{id}")
    public String take(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        Exam exam = examService.exam(id).orElse(null);
        if (exam == null) {
            return "redirect:/path";
        }
        if (!allowed(exam, user)) {
            ra.addFlashAttribute("toast", "Đề này dành cho học viên Premium.");
            return "redirect:/upgrade";
        }
        model.addAttribute("exam", exam);
        model.addAttribute("questions", examService.questionsFor(exam));
        return "exam/take";
    }

    @PostMapping("/exam/{id}/submit")
    public String submit(@PathVariable Long id, @RequestParam Map<String, String> params,
                         RedirectAttributes ra) {
        User user = currentUser.require();
        Exam exam = examService.exam(id).orElse(null);
        if (exam == null) {
            return "redirect:/path";
        }
        if (!allowed(exam, user)) {
            return "redirect:/upgrade";
        }
        ExamOutcome outcome = examService.grade(exam, PlacementController.parseAnswers(params));
        ra.addFlashAttribute("outcome", outcome);
        return "redirect:/exam/" + id + "/result";
    }

    @GetMapping("/exam/{id}/result")
    public String result(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("outcome")) {
            return "redirect:/exam/" + id;
        }
        return "exam/result";
    }
}
