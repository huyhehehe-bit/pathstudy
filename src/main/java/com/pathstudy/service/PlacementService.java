package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.PlacementResultRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.web.dto.PlacementOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PlacementService {

    public static final int ATTEMPTS_MAX = 3;

    private final QuestionRepository questions;
    private final PlacementResultRepository results;
    private final EnrollmentRepository enrollments;
    private final StudyPathService studyPath;

    public PlacementService(QuestionRepository questions, PlacementResultRepository results,
                            EnrollmentRepository enrollments, StudyPathService studyPath) {
        this.questions = questions;
        this.results = results;
        this.enrollments = enrollments;
        this.studyPath = studyPath;
    }

    @Transactional(readOnly = true)
    public List<Question> questionsFor(Subject subject) {
        return questions.findByScopeAndSubjectOrderByOrderIndexAsc(QuizScope.PLACEMENT, subject);
    }

    @Transactional(readOnly = true)
    public int attemptsUsed(User user, Subject subject) {
        return (int) results.countByUserAndSubject(user, subject);
    }

    @Transactional(readOnly = true)
    public boolean canAttempt(User user, Subject subject) {
        return attemptsUsed(user, subject) < ATTEMPTS_MAX;
    }

    @Transactional
    public PlacementOutcome grade(User user, Subject subject, Map<Long, Integer> answers) {
        List<Question> qs = questionsFor(subject);

        int correct = 0;
        Map<Competency, int[]> tally = new EnumMap<>(Competency.class); // [correct, total]
        for (Question q : qs) {
            int[] t = tally.computeIfAbsent(q.getCompetency(), k -> new int[2]);
            t[1]++;
            Integer a = answers.get(q.getId());
            if (a != null && a == q.getCorrectIndex()) {
                correct++;
                t[0]++;
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

        int attemptNo = attemptsUsed(user, subject) + 1;
        PlacementResult r = new PlacementResult();
        r.setUser(user);
        r.setSubject(subject);
        r.setAttemptNo(attemptNo);
        r.setScore(score);
        r.setLevel(level);
        r.setStrengths(String.join(", ", strengths));
        r.setWeaknesses(String.join(", ", weaknesses));
        results.save(r);

        // The system uses the BEST attempt to set the starting level.
        PlacementResult best = results.findTopByUserAndSubjectOrderByScoreDesc(user, subject).orElse(r);
        boolean bestUpdated = best.getId().equals(r.getId());

        Enrollment e = enrollments.findByUserAndSubject(user, subject)
                .orElseGet(() -> studyPath.enroll(user, subject, enrollments.countByUser(user) == 0));
        e.setLevel(best.getLevel());
        enrollments.save(e);

        // Build the personalized path now that we know the starting point.
        studyPath.ensurePathInitialized(user, subject);

        return new PlacementOutcome(subject, score, level, strengths, weaknesses,
                attemptNo, attemptNo, ATTEMPTS_MAX, bestUpdated);
    }

    public static String levelFor(int score) {
        if (score >= 80) return "Giỏi";
        if (score >= 65) return "Khá";
        if (score >= 50) return "Trung bình";
        return "Cần cải thiện";
    }
}
