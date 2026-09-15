package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.EstimateResultRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.web.dto.EstimateOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EstimateService {

    private final QuestionRepository questions;
    private final EstimateResultRepository results;
    private final StudyPathService studyPath;

    public EstimateService(QuestionRepository questions, EstimateResultRepository results,
                           StudyPathService studyPath) {
        this.questions = questions;
        this.results = results;
        this.studyPath = studyPath;
    }

    @Transactional(readOnly = true)
    public List<Question> questionsFor(CourseModule module) {
        return questions.findByScopeAndModuleOrderByOrderIndexAsc(QuizScope.ESTIMATE, module);
    }

    @Transactional(readOnly = true)
    public boolean hasEstimate(CourseModule module) {
        return !questionsFor(module).isEmpty();
    }

    @Transactional(readOnly = true)
    public Optional<EstimateResult> latest(User user, CourseModule module) {
        return results.findTopByUserAndModuleOrderByCreatedAtDesc(user, module);
    }

    @Transactional
    public EstimateOutcome grade(User user, CourseModule module, Map<Long, Integer> answers) {
        List<Question> qs = questionsFor(module);

        int correct = 0;
        Map<Competency, int[]> tally = new EnumMap<>(Competency.class);
        for (Question q : qs) {
            int[] t = tally.computeIfAbsent(q.getCompetency(), k -> new int[2]);
            t[1]++;
            Integer a = answers.get(q.getId());
            if (a != null && a == q.getCorrectIndex()) {
                correct++;
                t[0]++;
            }
        }

        int total = qs.isEmpty() ? 0 : Math.round(correct * 100f / qs.size());
        int knowledge = pct(tally, Competency.KNOWLEDGE);
        int analysis = pct(tally, Competency.ANALYSIS);
        int application = pct(tally, Competency.APPLICATION);
        boolean passed = total >= module.getPassThreshold();

        EstimateResult r = new EstimateResult();
        r.setUser(user);
        r.setModule(module);
        r.setTotal(total);
        r.setKnowledge(knowledge);
        r.setAnalysis(analysis);
        r.setApplication(application);
        r.setPassed(passed);
        results.save(r);

        // The path self-adjusts based on performance.
        CourseModule next;
        if (passed) {
            next = studyPath.completeModule(user, module);
        } else {
            studyPath.touchModuleProgress(user, module, Math.max(total, 40));
            next = studyPath.nextModule(module);
        }

        List<String> weakAreas = new ArrayList<>();
        for (Competency c : List.of(Competency.KNOWLEDGE, Competency.ANALYSIS, Competency.APPLICATION)) {
            if (tally.containsKey(c) && pct(tally, c) < module.getPassThreshold()) {
                weakAreas.add(c.getLabel());
            }
        }

        return new EstimateOutcome(r, module, next, passed, module.getPassThreshold(), weakAreas);
    }

    private int pct(Map<Competency, int[]> tally, Competency c) {
        int[] t = tally.get(c);
        if (t == null || t[1] == 0) return 0;
        return Math.round(t[0] * 100f / t[1]);
    }
}
