package com.pathstudy.repo;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModuleOrderByOrderIndexAsc(CourseModule module);
    Optional<Lesson> findFirstByModuleOrderByOrderIndexAsc(CourseModule module);
}
