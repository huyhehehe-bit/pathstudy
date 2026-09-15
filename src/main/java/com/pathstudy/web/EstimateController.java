package com.pathstudy.web;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.User;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.EstimateService;
import com.pathstudy.service.LearningService;
import com.pathstudy.web.dto.EstimateOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/module/{id}/estimate")
public class EstimateController {

    private final LearningService learning;
    private final EstimateService estimate;
    private final CurrentUserService currentUser;

    public EstimateController(LearningService learning, EstimateService estimate,
                              CurrentUserService currentUser) {
        this.learning = learning;
        this.estimate = estimate;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String intro(@PathVariable Long id, Model model, RedirectAttributes ra) {
        CourseModule module = learning.module(id);
        if (!estimate.hasEstimate(module)) {
            ra.addFlashAttribute("toast", "Module này chưa có bài kiểm tra ước lượng.");
            return "redirect:/module/" + id;
        }
        model.addAttribute("module", module);
        model.addAttribute("questionCount", estimate.questionsFor(module).size());
        return "estimate/intro";
    }

    @GetMapping("/test")
    public String test(@PathVariable Long id, Model model, RedirectAttributes ra) {
        CourseModule module = learning.module(id);
        if (!estimate.hasEstimate(module)) {
            return "redirect:/module/" + id;
        }
        model.addAttribute("module", module);
        model.addAttribute("questions", estimate.questionsFor(module));
        return "estimate/test";
    }

    @PostMapping("/submit")
    public String submit(@PathVariable Long id, @RequestParam Map<String, String> params,
                         RedirectAttributes ra) {
        User user = currentUser.require();
        CourseModule module = learning.module(id);
        EstimateOutcome outcome = estimate.grade(user, module, PlacementController.parseAnswers(params));
        ra.addFlashAttribute("outcome", outcome);
        return "redirect:/module/" + id + "/estimate/result";
    }

    @GetMapping("/result")
    public String result(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("outcome")) {
            return "redirect:/module/" + id + "/estimate";
        }
        return "estimate/result";
    }
}
