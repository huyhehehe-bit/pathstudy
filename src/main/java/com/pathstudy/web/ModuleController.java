package com.pathstudy.web;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.ProgressStatus;
import com.pathstudy.domain.User;
import com.pathstudy.service.BookmarkService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.LearningService;
import com.pathstudy.service.StudyPathService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/module/{id}")
public class ModuleController {

    private final LearningService learning;
    private final BookmarkService bookmarks;
    private final CurrentUserService currentUser;
    private final StudyPathService studyPath;

    public ModuleController(LearningService learning, BookmarkService bookmarks,
                            CurrentUserService currentUser, StudyPathService studyPath) {
        this.learning = learning;
        this.bookmarks = bookmarks;
        this.currentUser = currentUser;
        this.studyPath = studyPath;
    }

    @GetMapping
    public String view(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = currentUser.require();
        CourseModule module = learning.module(id);
        if (!module.isHasContent()) {
            ra.addFlashAttribute("toast", "Nội dung \"" + module.getTitle() + "\" đang được cập nhật.");
            return "redirect:/path?subject=" + module.getSubject().getCode();
        }
        // Gating: cannot open a locked module — finish the previous one first.
        if (studyPath.statusOf(user, module) == ProgressStatus.LOCKED) {
            ra.addFlashAttribute("toast", "Hãy hoàn thành phần trước để mở khoá bài học này.");
            return "redirect:/path?subject=" + module.getSubject().getCode();
        }
        model.addAttribute("detail", learning.openModule(user, id));
        return "module/view";
    }

    @PostMapping("/bookmark/{sectionId}")
    public String bookmark(@PathVariable Long id, @PathVariable Long sectionId, RedirectAttributes ra) {
        User user = currentUser.require();
        bookmarks.saveSection(user, sectionId);
        ra.addFlashAttribute("toast", "Đã lưu vào Kho lưu trữ của bạn.");
        return "redirect:/module/" + id + "#s" + sectionId;
    }

    @PostMapping("/note")
    public String note(@PathVariable Long id, @RequestParam String note,
                       @RequestParam(required = false) String sourceLabel, RedirectAttributes ra) {
        User user = currentUser.require();
        if (note != null && !note.isBlank()) {
            bookmarks.saveNote(user, id, sourceLabel, note);
            ra.addFlashAttribute("toast", "Đã lưu ghi chú.");
        }
        return "redirect:/module/" + id;
    }
}
