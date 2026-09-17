package com.pathstudy.service;

import java.util.List;

/**
 * Generates a personalized study plan from a learner's result and weak topics.
 * Implemented by an AI provider (Gemini); callers must handle a null return by
 * falling back to a rule-based plan.
 */
public interface AiStudyPlanService {

    boolean isEnabled();

    /** @return the plan text, or null if AI is disabled or the call failed. */
    String generatePlan(String subjectName, int score, String level, List<String> weakTopics);
}
