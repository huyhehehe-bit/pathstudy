package com.pathstudy.repo;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Question;
import com.pathstudy.domain.QuizScope;
import com.pathstudy.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByScopeAndSubjectOrderByOrderIndexAsc(QuizScope scope, Subject subject);
    List<Question> findByScopeAndSubjectAndGradeOrderByOrderIndexAsc(QuizScope scope, Subject subject, String grade);
    List<Question> findByScopeAndModuleOrderByOrderIndexAsc(QuizScope scope, CourseModule module);
    List<Question> findByExamOrderByOrderIndexAsc(Exam exam);
}
