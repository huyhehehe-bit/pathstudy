package com.pathstudy.repo;

import com.pathstudy.domain.EnglishLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnglishLessonRepository extends JpaRepository<EnglishLesson, Long> {

    List<EnglishLesson> findByGradeOrderByOrderIndexAsc(String grade);

    Optional<EnglishLesson> findByGradeAndTopic(String grade, String topic);
}
