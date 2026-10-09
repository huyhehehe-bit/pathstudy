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

    /**
     * Như {@link #generateExam} nhưng Gemini ĐỌC TRỰC TIẾP một file PDF/ảnh
     * (đề thi, trang SGK...) để bám sát nội dung file đó khi soạn câu hỏi.
     * @param fileBytes nội dung file, {@code mimeType} ví dụ "application/pdf".
     * @return danh sách câu hỏi hợp lệ (rỗng nếu AI tắt/lỗi).
     */
    List<GeneratedQuestion> generateExamFromFile(String subjectName, String grade, String topic,
                                                 int count, String difficulty,
                                                 byte[] fileBytes, String mimeType);

    /**
     * Giải thích MỘT câu trắc nghiệm cho học sinh vừa làm sai: vì sao đáp án đúng
     * là đúng, và vì sao lựa chọn của em sai.
     *
     * @param chosenIndex lựa chọn của học sinh, -1 nếu bỏ trống.
     * @param passage     đoạn văn dùng chung (câu đọc hiểu), có thể null.
     * @return lời giải thích, hoặc null nếu AI tắt/lỗi — caller phải tự xử lý.
     */
    String explainAnswer(String subjectName, String questionText, List<String> options,
                         int correctIndex, int chosenIndex, String passage);

    /** Admin diagnostic: human-readable status of the AI configuration + a live ping. */
    String diagnose();
}
