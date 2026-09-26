package com.pathstudy.service;

import java.util.List;

/**
 * Generates a personalized study plan from a learner's result and weak topics.
 * Implemented by an AI provider (Gemini); callers must handle a null return by
 * falling back to a rule-based plan.
 */
public interface AiStudyPlanService {

    boolean isEnabled();

    /**
     * Uses the learner's result + the teacher's reference material to synthesize
     * weaknesses, write a short lesson and generate practice questions.
     * @return the text, or null if AI is disabled or the call failed.
     */
    String generatePlan(String subjectName, int score, String level, List<String> weakTopics,
                        String referenceMaterial);

    /**
     * Sinh {@code count} câu hỏi trắc nghiệm (4 lựa chọn) cho một đề thi, theo môn/
     * khối/chủ đề/độ khó giáo viên yêu cầu, bám tài liệu nguồn nếu có.
     * @return danh sách câu hỏi hợp lệ (có thể ít hơn count, hoặc rỗng nếu AI tắt/lỗi).
     */
    List<GeneratedQuestion> generateExam(String subjectName, String grade, String topic,
                                         int count, String difficulty, String referenceMaterial);

    /** Admin diagnostic: human-readable status of the AI configuration + a live ping. */
    String diagnose();
}
