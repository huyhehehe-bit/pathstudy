package com.pathstudy.web;

import com.pathstudy.domain.Competency;
import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Exam;
import com.pathstudy.domain.MaterialType;
import com.pathstudy.domain.ReferenceMaterial;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.SubjectResource;
import com.pathstudy.repo.CourseModuleRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.repo.SubjectResourceRepository;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.ExamBuilderService;
import com.pathstudy.service.ExamService;
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

    /** Phân loại tài liệu cho kho tài liệu theo môn. */
    private static final List<String> RESOURCE_CATEGORIES = List.of(
            "Đề thi cuối kì", "Đề thi giữa kì", "Giáo trình", "Bài tập", "Khác");

    /** Khối cho đề thi ("" = chung mọi lớp). */
    private static final List<String> EXAM_GRADES = List.of("Lớp 10", "Lớp 11", "Lớp 12");
    private static final List<String> DIFFICULTIES = List.of("Cơ bản", "Trung bình", "Nâng cao");

    private final SubjectRepository subjects;
    private final CourseModuleRepository modules;
    private final MaterialService materials;
    private final CurrentUserService currentUser;
    private final QuestionBankService questionBank;
    private final ReferenceMaterialRepository referenceMaterials;
    private final SubjectResourceRepository subjectResources;
    private final ExamService examService;
    private final ExamBuilderService examBuilder;

    public TeacherController(SubjectRepository subjects, CourseModuleRepository modules,
                             MaterialService materials, CurrentUserService currentUser,
                             QuestionBankService questionBank,
                             ReferenceMaterialRepository referenceMaterials,
                             SubjectResourceRepository subjectResources,
                             ExamService examService, ExamBuilderService examBuilder) {
        this.subjects = subjects;
        this.modules = modules;
        this.materials = materials;
        this.currentUser = currentUser;
        this.questionBank = questionBank;
        this.referenceMaterials = referenceMaterials;
        this.subjectResources = subjectResources;
        this.examService = examService;
        this.examBuilder = examBuilder;
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

    // ---------- Reference documents (tài liệu nguồn cho AI) ----------

    @GetMapping("/docs")
    public String docs(@RequestParam(defaultValue = "anh") String subject, Model model) {
        Subject subj = subjects.findByCode(subject)
                .orElseGet(() -> subjects.findAllByOrderByOrderIndexAsc().get(0));
        model.addAttribute("subject", subj);
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("docs", referenceMaterials.findBySubjectOrderByIdAsc(subj));
        return "teacher/docs";
    }

    @PostMapping("/docs")
    public String addDoc(@RequestParam String subject, @RequestParam String title,
                         @RequestParam String content, RedirectAttributes ra) {
        Subject subj = subjects.findByCode(subject).orElseThrow();
        if (title.isBlank() || content.isBlank()) {
            ra.addFlashAttribute("toast", "Vui lòng nhập tiêu đề và nội dung tài liệu.");
            return "redirect:/teacher/docs?subject=" + subject;
        }
        ReferenceMaterial rm = new ReferenceMaterial();
        rm.setSubject(subj);
        rm.setTitle(title.strip());
        rm.setContent(content);
        rm.setCreatedByEmail(currentUser.current().map(u -> u.getEmail()).orElse(null));
        referenceMaterials.save(rm);
        ra.addFlashAttribute("toast", "Đã thêm tài liệu nguồn cho AI.");
        return "redirect:/teacher/docs?subject=" + subject;
    }

    @PostMapping("/docs/{id}/delete")
    public String deleteDoc(@PathVariable Long id, @RequestParam String subject, RedirectAttributes ra) {
        referenceMaterials.deleteById(id);
        ra.addFlashAttribute("toast", "Đã xoá tài liệu.");
        return "redirect:/teacher/docs?subject=" + subject;
    }

    // ---------- Kho tài liệu theo môn (link ngoài: Google Drive, PDF...) ----------

    @GetMapping("/resources")
    public String resources(@RequestParam(defaultValue = "van") String subject, Model model) {
        Subject subj = subjects.findByCode(subject)
                .orElseGet(() -> subjects.findAllByOrderByOrderIndexAsc().get(0));
        List<SubjectResource> all = subjectResources.findBySubjectOrderByCreatedAtDesc(subj);
        model.addAttribute("subject", subj);
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("categories", RESOURCE_CATEGORIES);
        model.addAttribute("resources", all);
        model.addAttribute("total", all.size());
        return "teacher/resources";
    }

    @PostMapping("/resources")
    public String addResource(@RequestParam String subject, @RequestParam String title,
                              @RequestParam String url, @RequestParam(required = false) String category,
                              @RequestParam(required = false) String description, RedirectAttributes ra) {
        Subject subj = subjects.findByCode(subject).orElseThrow();
        String link = url == null ? "" : url.strip();
        if (title.isBlank() || link.isBlank()) {
            ra.addFlashAttribute("toast", "Vui lòng nhập tiêu đề và liên kết tài liệu.");
            return "redirect:/teacher/resources?subject=" + subject;
        }
        // Chỉ chấp nhận http/https để tránh liên kết độc hại (javascript:, data:...).
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            ra.addFlashAttribute("toast", "Liên kết phải bắt đầu bằng http:// hoặc https://");
            return "redirect:/teacher/resources?subject=" + subject;
        }
        SubjectResource r = new SubjectResource();
        r.setSubject(subj);
        r.setTitle(title.strip());
        r.setUrl(link);
        r.setCategory(RESOURCE_CATEGORIES.contains(category) ? category : "Khác");
        r.setDescription(description == null || description.isBlank() ? null : description.strip());
        r.setCreatedByEmail(currentUser.current().map(u -> u.getEmail()).orElse(null));
        subjectResources.save(r);
        ra.addFlashAttribute("toast", "Đã thêm tài liệu vào kho.");
        return "redirect:/teacher/resources?subject=" + subject;
    }

    @PostMapping("/resources/{id}/delete")
    public String deleteResource(@PathVariable Long id, @RequestParam String subject,
                                 RedirectAttributes ra) {
        subjectResources.deleteById(id);
        ra.addFlashAttribute("toast", "Đã xoá tài liệu.");
        return "redirect:/teacher/resources?subject=" + subject;
    }

    // ---------- Tạo đề thi (AI soạn theo form) ----------

    @GetMapping("/exams")
    public String exams(@RequestParam(defaultValue = "van") String subject, Model model) {
        Subject subj = subjects.findByCode(subject)
                .orElseGet(() -> subjects.findAllByOrderByOrderIndexAsc().get(0));
        List<Exam> list = examService.listExams(subj);
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (Exam e : list) {
            counts.put(e.getId(), examService.questionsFor(e).size());
        }
        model.addAttribute("subject", subj);
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("exams", list);
        model.addAttribute("counts", counts);
        return "teacher/exams";
    }

    @GetMapping("/exams/new")
    public String examForm(@RequestParam(defaultValue = "van") String subject, Model model) {
        Subject subj = subjects.findByCode(subject)
                .orElseGet(() -> subjects.findAllByOrderByOrderIndexAsc().get(0));
        model.addAttribute("subject", subj);
        model.addAttribute("subjects", subjects.findAllByOrderByOrderIndexAsc());
        model.addAttribute("grades", EXAM_GRADES);
        model.addAttribute("difficulties", DIFFICULTIES);
        model.addAttribute("aiEnabled", examBuilder.aiEnabled());
        return "teacher/exam-new";
    }

    @PostMapping("/exams/generate")
    public String generateExam(@RequestParam String subject, @RequestParam String title,
                               @RequestParam(required = false) String grade,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String topic,
                               @RequestParam(defaultValue = "10") int count,
                               @RequestParam(required = false) String difficulty,
                               @RequestParam(defaultValue = "true") boolean premium,
                               RedirectAttributes ra) {
        Subject subj = subjects.findByCode(subject).orElseThrow();
        if (title.isBlank()) {
            ra.addFlashAttribute("toast", "Vui lòng nhập tên đề.");
            return "redirect:/teacher/exams/new?subject=" + subject;
        }
        int n = Math.max(1, Math.min(count, 40));
        String createdBy = currentUser.current().map(u -> u.getEmail()).orElse(null);
        Exam exam = examBuilder.createExam(subj, title, grade, description, premium, createdBy);
        if (examBuilder.aiEnabled()) {
            int added = examBuilder.generateQuestions(exam, topic, n, difficulty);
            ra.addFlashAttribute("toast", added > 0
                    ? "AI đã soạn " + added + " câu. Xem lại và chỉnh nếu cần."
                    : "AI chưa soạn được câu nào (thử lại hoặc thêm tay). Xem /admin/ai-check.");
        } else {
            ra.addFlashAttribute("toast", "AI chưa bật — đề đã tạo, hãy thêm câu hỏi thủ công.");
        }
        return "redirect:/teacher/exams/" + exam.getId();
    }

    @GetMapping("/exams/{id}")
    public String examView(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Exam exam = examService.exam(id).orElse(null);
        if (exam == null) {
            ra.addFlashAttribute("toast", "Không tìm thấy đề.");
            return "redirect:/teacher/exams";
        }
        model.addAttribute("exam", exam);
        model.addAttribute("questions", examService.questionsFor(exam));
        model.addAttribute("competencies", Competency.values());
        return "teacher/exam-view";
    }

    @PostMapping("/exams/{id}/question")
    public String addExamQuestion(@PathVariable Long id, @RequestParam String text,
                                  @RequestParam String optA, @RequestParam String optB,
                                  @RequestParam String optC, @RequestParam String optD,
                                  @RequestParam int correct, @RequestParam String competency,
                                  @RequestParam(required = false) String topic, RedirectAttributes ra) {
        Exam exam = examService.exam(id).orElseThrow();
        if (text.isBlank() || optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
            ra.addFlashAttribute("toast", "Vui lòng nhập câu hỏi và đủ 4 đáp án.");
            return "redirect:/teacher/exams/" + id;
        }
        examBuilder.addManualQuestion(exam, text, List.of(optA, optB, optC, optD),
                Math.max(0, Math.min(correct, 3)), Competency.valueOf(competency), topic);
        ra.addFlashAttribute("toast", "Đã thêm câu hỏi.");
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/exams/{id}/question/{qid}/delete")
    public String deleteExamQuestion(@PathVariable Long id, @PathVariable Long qid,
                                     RedirectAttributes ra) {
        examBuilder.deleteQuestion(qid);
        ra.addFlashAttribute("toast", "Đã xoá câu hỏi.");
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/exams/{id}/delete")
    public String deleteExam(@PathVariable Long id, @RequestParam String subject,
                             RedirectAttributes ra) {
        examService.exam(id).ifPresent(examBuilder::deleteExam);
        ra.addFlashAttribute("toast", "Đã xoá đề.");
        return "redirect:/teacher/exams?subject=" + subject;
    }
}
