package com.pathstudy.repo;

import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySubjectOrderByOrderIndexAsc(Subject subject);
}
