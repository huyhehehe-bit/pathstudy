package com.pathstudy.web;

import com.pathstudy.domain.User;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.FeedbackService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FeedbackController {

    private final FeedbackService feedback;
    private final CurrentUserService currentUser;

    public FeedbackController(FeedbackService feedback, CurrentUserService currentUser) {
        this.feedback = feedback;
        this.currentUser = currentUser;
    }

    @GetMapping("/feedback")
    public String form(Model model) {
        return "feedback/form";
    }

    @PostMapping("/feedback")
    public String submit(@RequestParam(required = false) Integer rating,
                         @RequestParam(required = false) String pros,
                         @RequestParam(required = false) String cons,
                         @RequestParam(required = false) String comment,
                         RedirectAttributes ra) {
        int r = rating == null ? 0 : Math.max(1, Math.min(5, rating));
        boolean empty = (pros == null || pros.isBlank())
                && (cons == null || cons.isBlank())
                && (comment == null || comment.isBlank());
        if (r == 0 && empty) {
            ra.addFlashAttribute("toast", "Hãy chọn số sao hoặc viết vài dòng nhé.");
            return "redirect:/feedback";
        }
        User user = currentUser.current().orElse(null);
        feedback.submit(user, r == 0 ? null : r, pros, cons, comment);
        ra.addFlashAttribute("toast", "Cảm ơn bạn đã góp ý! 💛");
        ra.addFlashAttribute("justSubmitted", true);
        ra.addFlashAttribute("submittedRating", r);
        return "redirect:/feedback";
    }
}
