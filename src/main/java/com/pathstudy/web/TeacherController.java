package com.pathstudy.web;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.MaterialType;
import com.pathstudy.domain.Subject;
import com.pathstudy.repo.CourseModuleRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.MaterialService;
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

    public TeacherController(SubjectRepository subjects, CourseModuleRepository modules,
                             MaterialService materials, CurrentUserService currentUser) {
        this.subjects = subjects;
        this.modules = modules;
        this.materials = materials;
        this.currentUser = currentUser;
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
}
