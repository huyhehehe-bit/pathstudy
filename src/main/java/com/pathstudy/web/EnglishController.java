package com.pathstudy.web;

import com.pathstudy.domain.EnglishLesson;
import com.pathstudy.domain.Exam;
import com.pathstudy.domain.PlacementResult;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.EnglishLessonRepository;
import com.pathstudy.repo.PlacementResultRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.repo.UserRepository;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.ExamService;
import com.pathstudy.web.dto.ExamOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/english")
public class EnglishController {

    /** Grades that have a curriculum available (SGK Global Success). */
    private static final List<String> GRADES = List.of("Lớp 10", "Lớp 11", "Lớp 12");

    private final ExamService examService;
    private final ReferenceMaterialRepository referenceMaterials;
    private final SubjectRepository subjects;
    private final CurrentUserService currentUser;
    private final BankTransferPaymentService payments;
    private final PlacementResultRepository placementResults;
    private final EnglishLessonRepository englishLessons;
    private final UserRepository users;

    public EnglishController(ExamService examService, ReferenceMaterialRepository referenceMaterials,
                             SubjectRepository subjects, CurrentUserService currentUser,
                             BankTransferPaymentService payments,
                             PlacementResultRepository placementResults,
                             EnglishLessonRepository englishLessons,
                             UserRepository users) {
        this.examService = examService;
        this.referenceMaterials = referenceMaterials;
        this.subjects = subjects;
        this.currentUser = currentUser;
        this.payments = payments;
        this.placementResults = placementResults;
        this.englishLessons = englishLessons;
        this.users = users;
    }

    private Subject anh() {
        return subjects.findByCode("anh").orElseThrow();
    }

    @GetMapping
    public String hub(Model model) {
        User user = currentUser.require();
        Subject anh = anh();

        // Khối là thuộc tính của học sinh (chọn khi đăng ký). Tài khoản cũ chưa có → cho chọn.
        String grade = user.getGrade();
        if (grade == null || !GRADES.contains(grade)) {
            model.addAttribute("grades", GRADES);
            return "english/choose-grade";
        }
        List<EnglishLesson> lessons = englishLessons.findByGradeOrderByOrderIndexAsc(grade);

        // Group lessons by unit for the curriculum browser.
        Map<String, List<EnglishLesson>> unitGroups = new LinkedHashMap<>();
        for (EnglishLesson l : lessons) {
            unitGroups.computeIfAbsent("Unit " + l.getUnitNo() + ": " + l.getUnitTitle(),
                    k -> new ArrayList<>()).add(l);
        }

        // Latest diagnostic (best attempt) for THIS grade → drive the personalized tutor path.
        PlacementResult placement = placementResults
                .findTopByUserAndSubjectAndGradeOrderByScoreDesc(user, anh, grade).orElse(null);
        List<String> weakTopics = parseWeakTopics(placement);
        List<EnglishLesson> recommended = new ArrayList<>();
        for (String topic : weakTopics) {
            englishLessons.findByGradeAndTopic(grade, topic).ifPresent(recommended::add);
        }

        model.addAttribute("grade", grade);
        model.addAttribute("grades", GRADES);
        model.addAttribute("exams", examService.listExams(anh));
        model.addAttribute("hasDocs", !referenceMaterials.findBySubjectOrderByIdAsc(anh).isEmpty());
        model.addAttribute("lessons", lessons);
        model.addAttribute("unitGroups", unitGroups);
        model.addAttribute("placement", placement);
        model.addAttribute("weakTopics", weakTopics);
        model.addAttribute("recommended", recommended);
        return "english/hub";
    }

    /** Đặt khối cho tài khoản cũ chưa có khối (đăng ký mới đã có sẵn). */
    @PostMapping("/grade")
    public String setGrade(@RequestParam String grade) {
        User user = currentUser.require();
        if (GRADES.contains(grade)) {
            user.setGrade(grade);
            users.save(user);
        }
        return "redirect:/english";
    }

    @GetMapping("/lesson/{id}")
    public String lesson(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        EnglishLesson lesson = englishLessons.findById(id)
                .filter(l -> GRADES.contains(l.getGrade())).orElse(null);
        if (lesson == null) {
            return "redirect:/english";
        }
        if (!payments.hasPremiumAccess(user)) {
            ra.addFlashAttribute("toast", "Bài học theo giáo trình dành cho học viên Premium.");
            return "redirect:/upgrade";
        }
        model.addAttribute("lesson", lesson);
        return "english/lesson";
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

    private static List<String> parseWeakTopics(PlacementResult placement) {
        List<String> topics = new ArrayList<>();
        if (placement != null && placement.getWeakTopics() != null
                && !placement.getWeakTopics().isBlank()) {
            for (String t : placement.getWeakTopics().split(",")) {
                String s = t.trim();
                if (!s.isEmpty()) {
                    topics.add(s);
                }
            }
        }
        return topics;
    }
}
