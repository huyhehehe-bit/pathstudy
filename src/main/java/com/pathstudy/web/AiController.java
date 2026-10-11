package com.pathstudy.web;

import com.pathstudy.domain.ReferenceMaterial;
import com.pathstudy.domain.Subject;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.AiStudyPlanService;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.web.dto.StudyPlanContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Soạn giáo trình bằng AI theo kiểu BẤT ĐỒNG BỘ.
 *
 * <p>Trước đây lời gọi Gemini nằm thẳng trong request nộp bài, nên học sinh
 * nhìn màn hình trắng ~20 giây trước khi thấy điểm. Giờ chấm bài xong là trả
 * kết quả ngay, trang kết quả tự gọi về đây để lấy giáo trình chi tiết.
 */
@Controller
public class AiController {

    /** Tiền tố khoá trong session. */
    private static final String KEY_PREFIX = "studyPlanCtx:";

    private final AiStudyPlanService ai;
    private final CurrentUserService currentUser;
    private final SubjectRepository subjects;
    private final ReferenceMaterialRepository referenceMaterials;

    public AiController(AiStudyPlanService ai, CurrentUserService currentUser,
                        SubjectRepository subjects, ReferenceMaterialRepository referenceMaterials) {
        this.ai = ai;
        this.currentUser = currentUser;
        this.subjects = subjects;
        this.referenceMaterials = referenceMaterials;
    }

    /**
     * Cất ngữ cảnh vào session, trả về khoá để trang kết quả gọi lại.
     * Trả null nếu AI đang tắt — khi đó khỏi hiện khung "đang soạn".
     */
    public static String stash(HttpSession session, AiStudyPlanService ai, StudyPlanContext ctx) {
        if (!ai.isEnabled() || session == null) {
            return null;
        }
        String key = UUID.randomUUID().toString();
        session.setAttribute(KEY_PREFIX + key, ctx);
        return key;
    }

    @PostMapping("/ai/study-plan")
    @ResponseBody
    public Map<String, Object> studyPlan(@RequestParam String key, HttpSession session) {
        currentUser.require(); // phải đăng nhập
        Map<String, Object> out = new LinkedHashMap<>();

        Object raw = session.getAttribute(KEY_PREFIX + key);
        // Dùng một lần rồi bỏ: tránh bấm F5 nhiều lần đốt quota Gemini.
        session.removeAttribute(KEY_PREFIX + key);

        if (!(raw instanceof StudyPlanContext ctx)) {
            out.put("ok", false);
            out.put("error", "Phiên đã hết hạn, hãy tải lại trang.");
            return out;
        }
        if (!ai.isEnabled()) {
            out.put("ok", false);
            out.put("error", "AI chưa được bật.");
            return out;
        }

        Subject subject = subjects.findByCode(ctx.subjectCode()).orElse(null);
        if (subject == null) {
            out.put("ok", false);
            out.put("error", "Không tìm thấy môn học.");
            return out;
        }
        String reference = referenceMaterials.findBySubjectOrderByIdAsc(subject).stream()
                .map(ReferenceMaterial::getContent)
                .collect(Collectors.joining("\n\n"));

        String plan = ai.generatePlan(
                subject.getName(), ctx.score(), ctx.level(), ctx.weakTopics(), reference);

        if (plan == null || plan.isBlank()) {
            out.put("ok", false);
            out.put("error", "AI chưa soạn được giáo trình lần này.");
            return out;
        }
        out.put("ok", true);
        out.put("plan", plan);
        return out;
    }
}
