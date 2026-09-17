package com.pathstudy.web;

import com.pathstudy.domain.Competency;
import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.MaterialType;
import com.pathstudy.domain.Subject;
import com.pathstudy.repo.CourseModuleRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.MaterialService;
import com.pathstudy.service.QuestionBankService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final SubjectRepository subjects;
    private final CourseModuleRepository modules;
    private final MaterialService materials;
    private final CurrentUserService currentUser;
    private final QuestionBankService questionBank;

    public TeacherController(SubjectRepository subjects, CourseModuleRepository modules,
                             MaterialService materials, CurrentUserService currentUser,
                             QuestionBankService questionBank) {
        this.subjects = subjects;
        this.modules = modules;
        this.materials = materials;
        this.currentUser = currentUser;
        this.questionBank = questionBank;
    }

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "van") String subject, Model model) {
        Subject subj = subjects.findByCode(subject).orElseGet(
                () -> subjects.findAllByOrderByOrderIndexAsc().get(0));
        List<CourseModule> mods = modules.findBySubjectOrderByOrderIndexAsc(subj);
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (CourseModule m : mods) {
            counts.put(m.getId(), materials.listForModule(m).size());
        }
        model.addAttribute("subject", subj);
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("modules", mods);
        model.addAttribute("counts", counts);
        return "teacher/index";
    }

    @GetMapping("/module/{id}")
    public String manage(@PathVariable Long id, Model model) {
        CourseModule module = modules.findById(id).orElseThrow();
        model.addAttribute("module", module);
        model.addAttribute("lesson", materials.lessonOf(module).orElse(null));
        model.addAttribute("materials", materials.listForModule(module));
        model.addAttribute("types", MaterialType.values());
        return "teacher/module";
    }

    @PostMapping("/module/{id}/material")
    public String addMaterial(@PathVariable Long id, @RequestParam String type,
                              @RequestParam String title, @RequestParam String url,
                              RedirectAttributes ra) {
        CourseModule module = modules.findById(id).orElseThrow();
        if (title.isBlank() || url.isBlank()) {
            ra.addFlashAttribute("toast", "Vui lòng nhập tiêu đề và đường dẫn.");
            return "redirect:/teacher/module/" + id;
        }
        materials.add(module, MaterialType.valueOf(type), title, url,
                currentUser.current().map(u -> u.getEmail()).orElse(null));
        ra.addFlashAttribute("toast", "Đã thêm tài liệu.");
        return "redirect:/teacher/module/" + id;
    }

    @PostMapping("/material/{materialId}/delete")
    public String deleteMaterial(@PathVariable Long materialId, @RequestParam Long moduleId,
                                 RedirectAttributes ra) {
        materials.delete(materialId);
        ra.addFlashAttribute("toast", "Đã xoá tài liệu.");
        return "redirect:/teacher/module/" + moduleId;
    }

    // ---------- Question bank (ngân hàng đề) ----------

    @GetMapping("/questions")
    public String questions(@RequestParam(defaultValue = "anh") String subject, Model model) {
        Subject subj = subjects.findByCode(subject)
                .orElseGet(() -> subjects.findAllByOrderByOrderIndexAsc().get(0));
        model.addAttribute("subject", subj);
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("questions", questionBank.list(subj));
        model.addAttribute("competencies", Competency.values());
        return "teacher/questions";
    }

    @PostMapping("/questions")
    public String addQuestion(@RequestParam String subject, @RequestParam String text,
                              @RequestParam String optA, @RequestParam String optB,
                              @RequestParam String optC, @RequestParam String optD,
                              @RequestParam int correct, @RequestParam String competency,
                              @RequestParam(required = false) String topic, RedirectAttributes ra) {
        Subject subj = subjects.findByCode(subject).orElseThrow();
        if (text.isBlank() || optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
            ra.addFlashAttribute("toast", "Vui lòng nhập câu hỏi và đủ 4 đáp án.");
            return "redirect:/teacher/questions?subject=" + subject;
        }
        questionBank.add(subj, text, List.of(optA, optB, optC, optD), correct,
                Competency.valueOf(competency), topic);
        ra.addFlashAttribute("toast", "Đã thêm câu hỏi vào ngân hàng đề.");
        return "redirect:/teacher/questions?subject=" + subject;
    }

    @PostMapping("/questions/{id}/delete")
    public String deleteQuestion(@PathVariable Long id, @RequestParam String subject,
                                 RedirectAttributes ra) {
        questionBank.delete(id);
        ra.addFlashAttribute("toast", "Đã xoá câu hỏi.");
        return "redirect:/teacher/questions?subject=" + subject;
    }
}
