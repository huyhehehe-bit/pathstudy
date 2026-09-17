package com.pathstudy.web;

import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.ExamService;
import com.pathstudy.web.dto.ExamOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/english")
public class EnglishController {

    private final ExamService examService;
    private final ReferenceMaterialRepository referenceMaterials;
    private final SubjectRepository subjects;
    private final CurrentUserService currentUser;
    private final BankTransferPaymentService payments;

    public EnglishController(ExamService examService, ReferenceMaterialRepository referenceMaterials,
                             SubjectRepository subjects, CurrentUserService currentUser,
                             BankTransferPaymentService payments) {
        this.examService = examService;
        this.referenceMaterials = referenceMaterials;
        this.subjects = subjects;
        this.currentUser = currentUser;
        this.payments = payments;
    }

    private Subject anh() {
        return subjects.findByCode("anh").orElseThrow();
    }

    @GetMapping
    public String hub(Model model) {
        currentUser.require();
        model.addAttribute("exams", examService.listExams(anh()));
        model.addAttribute("hasDocs", !referenceMaterials.findBySubjectOrderByIdAsc(anh()).isEmpty());
        return "english/hub";
    }

    @GetMapping("/study")
    public String study(Model model) {
        User user = currentUser.require();
        if (!payments.hasPremiumAccess(user)) {
            return "redirect:/upgrade";
        }
        model.addAttribute("docs", referenceMaterials.findBySubjectOrderByIdAsc(anh()));
        return "english/study";
    }

    @GetMapping("/exam/{id}")
    public String exam(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        Exam exam = examService.exam(id).filter(e -> e.getSubject().getCode().equals("anh")).orElse(null);
        if (exam == null) {
            return "redirect:/english";
        }
        if (exam.isPremium() && !payments.hasPremiumAccess(user)) {
            ra.addFlashAttribute("toast", "Đề này dành cho học viên Premium.");
            return "redirect:/upgrade";
        }
        model.addAttribute("exam", exam);
        model.addAttribute("questions", examService.questionsFor(exam));
        return "english/exam";
    }

    @PostMapping("/exam/{id}/submit")
    public String submit(@PathVariable Long id, @RequestParam Map<String, String> params,
                         RedirectAttributes ra) {
        User user = currentUser.require();
        Exam exam = examService.exam(id).filter(e -> e.getSubject().getCode().equals("anh")).orElse(null);
        if (exam == null) {
            return "redirect:/english";
        }
        if (exam.isPremium() && !payments.hasPremiumAccess(user)) {
            return "redirect:/upgrade";
        }
        ExamOutcome outcome = examService.grade(exam, PlacementController.parseAnswers(params));
        ra.addFlashAttribute("outcome", outcome);
        return "redirect:/english/exam/" + id + "/result";
    }

    @GetMapping("/exam/{id}/result")
    public String result(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("outcome")) {
            return "redirect:/english/exam/" + id;
        }
        return "english/result";
    }
}
