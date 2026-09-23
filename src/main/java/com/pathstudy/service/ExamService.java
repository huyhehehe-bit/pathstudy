package com.pathstudy.service;

import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Question;
import com.pathstudy.domain.ReferenceMaterial;
import com.pathstudy.domain.Subject;
import com.pathstudy.repo.ExamRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.web.dto.ExamOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final ReferenceMaterialRepository referenceMaterials;
    private final AiStudyPlanService aiStudyPlan;

    public ExamService(ExamRepository exams, QuestionRepository questions,
                       ReferenceMaterialRepository referenceMaterials, AiStudyPlanService aiStudyPlan) {
        this.exams = exams;
        this.questions = questions;
        this.referenceMaterials = referenceMaterials;
        this.aiStudyPlan = aiStudyPlan;
    }

    @Transactional(readOnly = true)
    public List<Exam> listExams(Subject subject) {
        return exams.findBySubjectOrderByOrderIndexAsc(subject);
    }

    /** Đề luyện tập của khối {@code grade} + đề chung (grade == null); KHÔNG gồm đề THPT. */
    @Transactional(readOnly = true)
    public List<Exam> listExams(Subject subject, String grade) {
        List<Exam> result = new ArrayList<>();
        for (Exam e : exams.findBySubjectOrderByOrderIndexAsc(subject)) {
            if (e.getCategory() == null && (e.getGrade() == null || e.getGrade().equals(grade))) {
                result.add(e);
            }
        }
        return result;
    }

    /** Đề thi THPT Quốc gia — mục riêng, mọi lớp đều xem được. */
    @Transactional(readOnly = true)
    public List<Exam> listNationalExams(Subject subject) {
        List<Exam> result = new ArrayList<>();
        for (Exam e : exams.findBySubjectOrderByOrderIndexAsc(subject)) {
            if ("THPT".equals(e.getCategory())) {
                result.add(e);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<Exam> exam(Long id) {
        return exams.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Question> questionsFor(Exam exam) {
        return questions.findByExamOrderByOrderIndexAsc(exam);
    }

    @Transactional(readOnly = true)
    public ExamOutcome grade(Exam exam, Map<Long, Integer> answers) {
        List<Question> qs = questionsFor(exam);
        int correct = 0;
        Map<String, int[]> topicTally = new LinkedHashMap<>();
        for (Question q : qs) {
            Integer a = answers.get(q.getId());
            boolean ok = a != null && a == q.getCorrectIndex();
            if (ok) {
                correct++;
            }
            String topic = q.getTopic();
            if (topic != null && !topic.isBlank()) {
                int[] t = topicTally.computeIfAbsent(topic, k -> new int[2]);
                t[1]++;
                if (ok) t[0]++;
            }
        }

        int score = qs.isEmpty() ? 0 : Math.round(correct * 100f / qs.size());
        String level = PlacementService.levelFor(score);

        List<String> weakTopics = new ArrayList<>();
        for (Map.Entry<String, int[]> en : topicTally.entrySet()) {
            int[] t = en.getValue();
            int pct = t[1] == 0 ? 0 : Math.round(t[0] * 100f / t[1]);
            if (pct < 60) {
                weakTopics.add(en.getKey());
            }
        }

        String referenceText = referenceMaterials.findBySubjectOrderByIdAsc(exam.getSubject()).stream()
                .map(ReferenceMaterial::getContent).collect(Collectors.joining("\n\n"));
        String plan = aiStudyPlan.isEnabled()
                ? aiStudyPlan.generatePlan(exam.getSubject().getName(), score, level, weakTopics, referenceText)
                : null;
        if (plan == null) {
            plan = rulePlan(weakTopics);
        }

        return new ExamOutcome(exam, qs.size(), correct, score, level, weakTopics, plan);
    }

    private String rulePlan(List<String> weakTopics) {
        if (weakTopics.isEmpty()) {
            return "Bạn làm tốt! Hãy luyện thêm đề để giữ phong độ và mở rộng vốn từ.";
        }
        return "Tập trung ôn các chủ đề còn yếu: " + String.join(", ", weakTopics) + ".\n"
                + "• Ôn kỹ lý thuyết từng chủ đề (xem mục Học theo giáo trình).\n"
                + "• Làm lại các câu sai và tìm 15–20 câu tương tự để luyện.\n"
                + "• Bổ sung từ vựng theo chủ điểm mỗi ngày.\n"
                + "(Bật AI Gemini để nhận giáo trình + bài tập chi tiết cho từng chủ đề yếu.)";
    }
}
