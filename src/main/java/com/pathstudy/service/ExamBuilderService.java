package com.pathstudy.service;

import com.pathstudy.domain.Competency;
import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Question;
import com.pathstudy.domain.QuizScope;
import com.pathstudy.domain.ReferenceMaterial;
import com.pathstudy.domain.Subject;
import com.pathstudy.repo.ExamRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cho phép giáo viên/admin tạo đề thi (Exam) và để AI (Gemini) soạn câu hỏi theo
 * form. Câu hỏi của đề dùng scope ESTIMATE + link exam (như đề seed), không gắn
 * subject/module nên không lọt vào truy vấn placement/module.
 */
@Service
public class ExamBuilderService {

    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final ReferenceMaterialRepository referenceMaterials;
    private final AiStudyPlanService ai;

    public ExamBuilderService(ExamRepository exams, QuestionRepository questions,
                              ReferenceMaterialRepository referenceMaterials, AiStudyPlanService ai) {
        this.exams = exams;
        this.questions = questions;
        this.referenceMaterials = referenceMaterials;
        this.ai = ai;
    }

    public boolean aiEnabled() {
        return ai.isEnabled();
    }

    @Transactional
    public Exam createExam(Subject subject, String title, String grade, String description,
                           boolean premium, String createdByEmail) {
        Exam e = new Exam();
        e.setSubject(subject);
        e.setTitle(title.strip());
        e.setLevel(grade == null || grade.isBlank() ? "Chung" : grade);
        e.setGrade(grade == null || grade.isBlank() ? null : grade);
        e.setDescription(description == null || description.isBlank() ? null : description.strip());
        e.setPremium(premium);
        e.setCategory(null); // đề luyện tập thường (hiện trong mục "Làm đề" theo khối)
        e.setCreatedByEmail(createdByEmail);
        e.setOrderIndex((int) (exams.findBySubjectOrderByOrderIndexAsc(subject).size() + 100));
        return exams.save(e);
    }

    /** Gọi AI soạn câu hỏi theo form và lưu vào đề. Trả về số câu đã thêm. */
    @Transactional
    public int generateQuestions(Exam exam, String topic, int count, String difficulty) {
        String referenceText = referenceMaterials.findBySubjectOrderByIdAsc(exam.getSubject()).stream()
                .map(ReferenceMaterial::getContent).collect(Collectors.joining("\n\n"));
        String grade = exam.getGrade();
        List<GeneratedQuestion> generated = ai.generateExam(
                exam.getSubject().getName(), grade, topic, count, difficulty, referenceText);
        int start = questions.findByExamOrderByOrderIndexAsc(exam).size();
        int added = 0;
        for (GeneratedQuestion g : generated) {
            addQuestion(exam, start + added + 1, g.text(), g.options(), g.correctIndex(),
                    parseCompetency(g.competency()), g.topic());
            added++;
        }
        return added;
    }

    @Transactional
    public void addManualQuestion(Exam exam, String text, List<String> options, int correctIndex,
                                  Competency competency, String topic) {
        int idx = questions.findByExamOrderByOrderIndexAsc(exam).size() + 1;
        addQuestion(exam, idx, text, options, correctIndex, competency, topic);
    }

    private void addQuestion(Exam exam, int idx, String text, List<String> options, int correctIndex,
                             Competency competency, String topic) {
        Question q = new Question();
        q.setScope(QuizScope.ESTIMATE);
        q.setExam(exam);
        q.setOrderIndex(idx);
        q.setText(text.strip());
        q.setOptions(options);
        q.setCorrectIndex(correctIndex);
        q.setCompetency(competency);
        q.setTopic(topic == null || topic.isBlank() ? null : topic.replace(",", " ").strip());
        questions.save(q);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        questions.deleteById(questionId);
    }

    @Transactional
    public void deleteExam(Exam exam) {
        questions.deleteAll(questions.findByExamOrderByOrderIndexAsc(exam));
        exams.delete(exam);
    }

    private static Competency parseCompetency(String name) {
        if (name != null) {
            for (Competency c : Competency.values()) {
                if (c.name().equalsIgnoreCase(name.strip())) {
                    return c;
                }
            }
        }
        return Competency.KNOWLEDGE;
    }
}
