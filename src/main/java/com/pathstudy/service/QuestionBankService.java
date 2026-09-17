package com.pathstudy.service;

import com.pathstudy.domain.Competency;
import com.pathstudy.domain.Question;
import com.pathstudy.domain.QuizScope;
import com.pathstudy.domain.Subject;
import com.pathstudy.repo.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Teacher-facing question bank. Teachers add tagged questions here; the entrance
 * / diagnostic test draws from this pool. Each question carries a fine-grained
 * topic (e.g. "Thì quá khứ tiếp diễn") so weakness analysis can be specific.
 */
@Service
public class QuestionBankService {

    private final QuestionRepository questions;

    public QuestionBankService(QuestionRepository questions) {
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public List<Question> list(Subject subject) {
        return questions.findByScopeAndSubjectOrderByOrderIndexAsc(QuizScope.PLACEMENT, subject);
    }

    @Transactional
    public Question add(Subject subject, String text, List<String> options, int correctIndex,
                        Competency competency, String topic) {
        Question q = new Question();
        q.setScope(QuizScope.PLACEMENT);
        q.setSubject(subject);
        q.setOrderIndex(list(subject).size() + 1);
        q.setText(text.strip());
        q.setOptions(options);
        q.setCorrectIndex(correctIndex);
        q.setCompetency(competency);
        q.setTopic(topic == null || topic.isBlank() ? null : topic.strip());
        return questions.save(q);
    }

    @Transactional
    public void delete(Long id) {
        questions.deleteById(id);
    }
}
