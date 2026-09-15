package com.pathstudy.repo;

import com.pathstudy.domain.Lesson;
import com.pathstudy.domain.LessonSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonSectionRepository extends JpaRepository<LessonSection, Long> {
    List<LessonSection> findByLessonOrderByOrderIndexAsc(Lesson lesson);
}
