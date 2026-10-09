package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.PlacementResultRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.web.dto.AnswerReview;
import com.pathstudy.web.dto.PlacementOutcome;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlacementService {

    public static final int ATTEMPTS_MAX = 3;

    /** Số câu mỗi LẦN làm bài. Nếu ngân hàng nhiều hơn số này → mỗi lần random khác
     *  nhau (hết lặp). Chỉnh qua env PLACEMENT_TEST_SIZE. */
    @Value("${app.placement.test-size:20}")
    private int testSize;

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

    /** Toàn bộ ngân hàng câu hỏi placement của môn/khối (grade null/blank = tất cả). */
    @Transactional(readOnly = true)
    public List<Question> questionsFor(Subject subject, String grade) {
        if (grade == null || grade.isBlank()) {
            return questionsFor(subject);
        }
        return questions.findByScopeAndSubjectAndGradeOrderByOrderIndexAsc(QuizScope.PLACEMENT, subject, grade);
    }

    /**
     * Bộ đề cho MỘT lần làm: RANDOM từ ngân hàng (giữ nguyên nhóm câu dùng chung
     * đoạn văn), tối đa {@code testSize} câu. Ngân hàng > testSize → mỗi lần khác
     * nhau; ngân hàng ≤ testSize → lấy hết nhưng xáo trộn thứ tự nhóm.
     */
    @Transactional(readOnly = true)
    public List<Question> randomTestFor(Subject subject, String grade) {
        return sample(questionsFor(subject, grade), Math.max(1, testSize));
    }

    /** Số câu một lần test sẽ hiện (để trang intro hiển thị + guard 0 câu). */
    @Transactional(readOnly = true)
    public int testQuestionCount(Subject subject, String grade) {
        return Math.min(questionsFor(subject, grade).size(), Math.max(1, testSize));
    }

    /** Chọn ngẫu nhiên ~size câu, giữ các câu cùng đoạn văn (passage) liền nhau. */
    private List<Question> sample(List<Question> pool, int size) {
        if (pool.size() <= size) {
            // Không đủ để bớt — vẫn xáo nhóm để thứ tự đỡ lặp giữa các lần.
            List<List<Question>> gs = groupByPassage(pool);
            Collections.shuffle(gs);
            List<Question> out = new ArrayList<>();
            gs.forEach(out::addAll);
            return out;
        }
        List<List<Question>> groups = groupByPassage(pool);
        Collections.shuffle(groups);
        List<Question> out = new ArrayList<>();
        for (List<Question> g : groups) {
            if (out.size() >= size) {
                break;
            }
            out.addAll(g);
        }
        return out;
    }

    /** Gom các câu LIÊN TIẾP dùng chung đoạn văn thành 1 nhóm; câu lẻ = nhóm 1 câu. */
    private List<List<Question>> groupByPassage(List<Question> pool) {
        List<List<Question>> groups = new ArrayList<>();
        for (Question q : pool) {
            String p = q.getPassage();
            if (p != null && !p.isBlank() && !groups.isEmpty()
                    && p.equals(groups.get(groups.size() - 1).get(0).getPassage())) {
                groups.get(groups.size() - 1).add(q);
            } else {
                List<Question> g = new ArrayList<>();
                g.add(q);
                groups.add(g);
            }
        }
        return groups;
    }

    @Transactional(readOnly = true)
    public int attemptsUsed(User user, Subject subject) {
        return (int) results.countByUserAndSubject(user, subject);
    }

    @Transactional(readOnly = true)
    public int attemptsUsed(User user, Subject subject, String grade) {
        if (grade == null || grade.isBlank()) {
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
        if (grade != null && grade.isBlank()) {
            grade = null;
        }
        // Chấm đúng những câu HỌC SINH ĐÃ LÀM (lọc từ ngân hàng theo id đã nộp),
        // không phụ thuộc bộ random đã hiển thị. Cũng loại id giả nếu có.
        List<Question> qs = questionsFor(subject, grade).stream()
                .filter(q -> answers.containsKey(q.getId()))
                .collect(Collectors.toList());

        int correct = 0;
        Map<Competency, int[]> tally = new EnumMap<>(Competency.class); // [correct, total]
        Map<String, int[]> topicTally = new LinkedHashMap<>();          // topic -> [correct, total]
        List<AnswerReview> reviews = new ArrayList<>();
        int number = 0;
        for (Question q : qs) {
            Integer a = answers.get(q.getId());
            boolean ok = a != null && a == q.getCorrectIndex();
            if (ok) {
                correct++;
            }
            reviews.add(new AnswerReview(q.getId(), ++number, q.getText(), q.getOptions(),
                    q.getCorrectIndex(), a == null ? -1 : a, q.getTopic(), q.getPassage()));
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
            studyPlan = rulePlan(subject, weakTopics);
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
                attemptNo, attemptNo, ATTEMPTS_MAX, bestUpdated, weakTopics, studyPlan, reviews);
    }

    private String rulePlan(Subject subject, List<String> weakTopics) {
        String code = subject == null ? null : subject.getCode();
        if (weakTopics.isEmpty()) {
            return "Bạn khá đều các phần. " + StudyAdvice.strongLine(code);
        }
        return "Tập trung ôn các chủ đề còn yếu: " + String.join(", ", weakTopics) + ".\n"
                + "• Ôn kỹ lý thuyết từng chủ đề trên kèm ví dụ.\n"
                + "• Làm 15–20 câu bài tập mỗi chủ đề để củng cố.\n"
                + "• " + StudyAdvice.practiceLine(code) + "\n"
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
