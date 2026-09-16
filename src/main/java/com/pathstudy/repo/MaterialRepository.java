package com.pathstudy.repo;

import com.pathstudy.domain.Lesson;
import com.pathstudy.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByLessonOrderByOrderIndexAscIdAsc(Lesson lesson);
    long countByLesson(Lesson lesson);
}
