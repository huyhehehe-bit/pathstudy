package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.PlacementResultRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.web.dto.PlacementOutcome;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlacementService {

    public static final int ATTEMPTS_MAX = 3;

    private final QuestionRepository questions;
    private final PlacementResultRepository results;
    private final EnrollmentRepository enrollments;
    private final StudyPathService studyPath;
    private final AiStudyPlanService aiStudyPlan;
    private final ReferenceMaterialRepository referenceMaterials;

    public PlacementService(QuestionRepository questions, PlacementResultRepository results,
                            EnrollmentRepository enrollments, StudyPathService studyPath,
                            AiStudyPlanService aiStudyPlan, ReferenceMaterialRepository referenceMaterials) {
        this.questions = questions;
        this.results = results;
        this.enrollments = enrollments;
        this.studyPath = studyPath;
        this.aiStudyPlan = aiStudyPlan;
        this.referenceMaterials = referenceMaterials;
    }

    @Transactional(readOnly = true)
    public List<Question> questionsFor(Subject subject) {
        return questions.findByScopeAndSubjectOrderByOrderIndexAsc(QuizScope.PLACEMENT, subject);
    }

    /** Grade-scoped question set (English). When grade is null, returns all. */
    @Transactional(readOnly = true)
    public List<Question> questionsFor(Subject subject, String grade) {
        if (grade == null) {
            return questionsFor(subject);
        }
        return questions.findByScopeAndSubjectAndGradeOrderByOrderIndexAsc(QuizScope.PLACEMENT, subject, grade);
    }

    @Transactional(readOnly = true)
    public int attemptsUsed(User user, Subject subject) {
        return (int) results.countByUserAndSubject(user, subject);
    }

    @Transactional(readOnly = true)
    public int attemptsUsed(User user, Subject subject, String grade) {
        if (grade == null) {
            return attemptsUsed(user, subject);
        }
        return (int) results.countByUserAndSubjectAndGrade(user, subject, grade);
    }

    @Transactional(readOnly = true)
    public boolean canAttempt(User user, Subject subject) {
        return attemptsUsed(user, subject) < ATTEMPTS_MAX;
    }

    @Transactional(readOnly = true)
    public boolean canAttempt(User user, Subject subject, String grade) {
        return attemptsUsed(user, subject, grade) < ATTEMPTS_MAX;
    }

    @Transactional
    public PlacementOutcome grade(User user, Subject subject, Map<Long, Integer> answers) {
        return grade(user, subject, null, answers);
    }

    @Transactional
    public PlacementOutcome grade(User user, Subject subject, String grade, Map<Long, Integer> answers) {
        List<Question> qs = questionsFor(subject, grade);

        int correct = 0;
        Map<Competency, int[]> tally = new EnumMap<>(Competency.class); // [correct, total]
        Map<String, int[]> topicTally = new LinkedHashMap<>();          // topic -> [correct, total]
        for (Question q : qs) {
            Integer a = answers.get(q.getId());
            boolean ok = a != null && a == q.getCorrectIndex();
            if (ok) {
                correct++;
            }
            int[] c = tally.computeIfAbsent(q.getCompetency(), k -> new int[2]);
            c[1]++;
            if (ok) c[0]++;

            String topic = q.getTopic();
            if (topic != null && !topic.isBlank()) {
                int[] t = topicTally.computeIfAbsent(topic, k -> new int[2]);
                t[1]++;
                if (ok) t[0]++;
            }
        }

        int score = qs.isEmpty() ? 0 : Math.round(correct * 100f / qs.size());
        String level = levelFor(score);

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        for (Map.Entry<Competency, int[]> en : tally.entrySet()) {
            int[] t = en.getValue();
            int pct = t[1] == 0 ? 0 : Math.round(t[0] * 100f / t[1]);
            if (pct >= 70) {
                strengths.add(en.getKey().getLabel());
            } else {
                weaknesses.add(en.getKey().getLabel());
            }
        }

        // Fine-grained weak topics (topics answered below 60%).
        List<String> weakTopics = new ArrayList<>();
        for (Map.Entry<String, int[]> en : topicTally.entrySet()) {
            int[] t = en.getValue();
            int pct = t[1] == 0 ? 0 : Math.round(t[0] * 100f / t[1]);
            if (pct < 60) {
                weakTopics.add(en.getKey());
            }
        }

        // Personalized study plan: AI (Gemini) grounded in the teacher's reference
        // material when enabled, else a rule-based plan.
        String referenceText = referenceMaterials.findBySubjectOrderByIdAsc(subject).stream()
                .map(ReferenceMaterial::getContent).collect(Collectors.joining("\n\n"));
        String studyPlan = aiStudyPlan.isEnabled()
                ? aiStudyPlan.generatePlan(subject.getName(), score, level, weakTopics, referenceText) : null;
        if (studyPlan == null) {
            studyPlan = rulePlan(weakTopics);
        }

        int attemptNo = attemptsUsed(user, subject, grade) + 1;
        PlacementResult r = new PlacementResult();
        r.setUser(user);
        r.setSubject(subject);
        r.setGrade(grade);
        r.setAttemptNo(attemptNo);
        r.setScore(score);
        r.setLevel(level);
        r.setStrengths(String.join(", ", strengths));
        r.setWeaknesses(String.join(", ", weaknesses));
        r.setWeakTopics(String.join(", ", weakTopics));
        r.setStudyPlan(studyPlan);
        results.save(r);

        // The system uses the BEST attempt (within the same grade) to set the starting level.
        PlacementResult best = (grade == null
                ? results.findTopByUserAndSubjectOrderByScoreDesc(user, subject)
                : results.findTopByUserAndSubjectAndGradeOrderByScoreDesc(user, subject, grade)).orElse(r);
        boolean bestUpdated = best.getId().equals(r.getId());

        Enrollment e = enrollments.findByUserAndSubject(user, subject)
                .orElseGet(() -> studyPath.enroll(user, subject, enrollments.countByUser(user) == 0));
        e.setLevel(best.getLevel());
        enrollments.save(e);

        // Build the personalized path now that we know the starting point.
        studyPath.ensurePathInitialized(user, subject);

        return new PlacementOutcome(subject, score, level, strengths, weaknesses,
                attemptNo, attemptNo, ATTEMPTS_MAX, bestUpdated, weakTopics, studyPlan);
    }

    private String rulePlan(List<String> weakTopics) {
        if (weakTopics.isEmpty()) {
            return "Bạn khá đều các phần. Hãy luyện đề tổng hợp để nâng điểm và bổ sung 20–30 từ vựng mỗi tuần.";
        }
        return "Tập trung ôn các chủ đề còn yếu: " + String.join(", ", weakTopics) + ".\n"
                + "• Ôn kỹ lý thuyết từng chủ đề trên kèm ví dụ.\n"
                + "• Làm 15–20 câu bài tập mỗi chủ đề để củng cố.\n"
                + "• Bổ sung 20–30 từ vựng mỗi tuần theo chủ điểm.\n"
                + "• Làm lại đề sau 1 tuần để đo tiến bộ.\n"
                + "(Bật AI Gemini để nhận giáo trình + bài tập chi tiết cho từng chủ đề yếu.)";
    }

    public static String levelFor(int score) {
        if (score >= 80) return "Giỏi";
        if (score >= 65) return "Khá";
        if (score >= 50) return "Trung bình";
        return "Cần cải thiện";
    }
}
