package com.pathstudy.web;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.ProgressStatus;
import com.pathstudy.domain.User;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.EstimateService;
import com.pathstudy.service.LearningService;
import com.pathstudy.service.StudyPathService;
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
    private final StudyPathService studyPath;
    private final BankTransferPaymentService payments;

    public EstimateController(LearningService learning, EstimateService estimate,
                              CurrentUserService currentUser, StudyPathService studyPath,
                              BankTransferPaymentService payments) {
        this.learning = learning;
        this.estimate = estimate;
        this.currentUser = currentUser;
        this.studyPath = studyPath;
        this.payments = payments;
    }

    /**
     * Bài tự đánh giá nằm TRONG một chương, nên phải chịu đúng hai cổng của
     * chương đó: chưa mở khoá thì không được làm, và chương Lớp 11/12 thì phải
     * có Premium.
     *
     * <p>Thiếu hai cổng này, /module/{id}/estimate trở thành cửa sau: học sinh
     * không trả phí vẫn mở được đề của chương khoá, làm xong đạt điểm là
     * {@code completeModule} đánh dấu hoàn thành và mở chương kế tiếp — đi hết
     * cả lộ trình mà không qua trang nâng cấp lần nào.
     *
     * @return null nếu được phép, hoặc chuỗi redirect nếu bị chặn.
     */
    private String chanNeuKhongDuocVao(User user, CourseModule module) {
        if (studyPath.statusOf(user, module) == ProgressStatus.LOCKED) {
            return "redirect:/path?subject=" + module.getSubject().getCode();
        }
        if (module.isPremiumContent() && !payments.hasPremiumAccess(user)) {
            return "redirect:/upgrade";
        }
        return null;
    }

    @GetMapping
    public String intro(@PathVariable Long id, Model model, RedirectAttributes ra) {
        CourseModule module = learning.findModule(id).orElse(null);
        if (module == null) {
            ra.addFlashAttribute("toast", "Không tìm thấy bài học này.");
            return "redirect:/path";
        }
        if (!estimate.hasEstimate(module)) {
            ra.addFlashAttribute("toast", "Module này chưa có bài kiểm tra ước lượng.");
            return "redirect:/module/" + id;
        }
        String chan = chanNeuKhongDuocVao(currentUser.require(), module);
        if (chan != null) {
            ra.addFlashAttribute("toast", module.isPremiumContent()
                    ? "Nội dung Lớp 11 & 12 dành cho học viên Premium."
                    : "Hãy hoàn thành phần trước để mở khoá bài này.");
            return chan;
        }
        model.addAttribute("module", module);
        model.addAttribute("questionCount", estimate.questionsFor(module).size());
        return "estimate/intro";
    }

    @GetMapping("/test")
    public String test(@PathVariable Long id, Model model, RedirectAttributes ra) {
        CourseModule module = learning.findModule(id).orElse(null);
        if (module == null) {
            return "redirect:/path";
        }
        if (!estimate.hasEstimate(module)) {
            return "redirect:/module/" + id;
        }
        String chan = chanNeuKhongDuocVao(currentUser.require(), module);
        if (chan != null) {
            return chan;
        }
        model.addAttribute("module", module);
        model.addAttribute("questions", estimate.questionsFor(module));
        return "estimate/test";
    }

    @PostMapping("/submit")
    public String submit(@PathVariable Long id, @RequestParam Map<String, String> params,
                         RedirectAttributes ra) {
        User user = currentUser.require();
        CourseModule module = learning.findModule(id).orElse(null);
        if (module == null) {
            return "redirect:/path";
        }
        // Chặn cả ở bước nộp: không có cổng ở đây thì gửi thẳng POST là vẫn
        // chấm điểm và mở khoá chương, khỏi cần mở trang làm bài.
        String chan = chanNeuKhongDuocVao(user, module);
        if (chan != null) {
            return chan;
        }
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
